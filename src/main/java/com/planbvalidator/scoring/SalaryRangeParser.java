package com.planbvalidator.scoring;

import com.planbvalidator.config.CurrencyRatesProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses free-text salary ranges (INR LPA, foreign currency, monthly/annual amounts)
 * and normalizes them to monthly INR and ₹X-Y LPA display form.
 */
@Component
public class SalaryRangeParser {

    private static final double LPA_TO_ANNUAL_INR = 100_000.0;
    private static final Pattern LPA_RANGE = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*(?:-|–|to)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:lpa|l\\.?p\\.?a|lakh|lac)s?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LPA_SINGLE = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*(?:lpa|l\\.?p\\.?a|lakh|lac)s?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMERIC_RANGE = Pattern.compile(
            "(\\d[\\d,]*(?:\\.\\d+)?)\\s*(?:-|–|to)\\s*(?:₹|\\$|€|£|[A-Z]{3}\\s+)?(\\d[\\d,]*(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LONE_NUMBER = Pattern.compile("(\\d[\\d,]*(?:\\.\\d+)?)");
    private static final Pattern MONTHLY_HINT = Pattern.compile(
            "(?:per\\s*month|/\\s*month|monthly|p\\.?m\\.?)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ANNUAL_HINT = Pattern.compile(
            "(?:per\\s*(?:year|annum)|annually|p\\.?a\\.?|/\\s*(?:year|yr|annum))",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CURRENCY_CODE = Pattern.compile(
            "\\b(AED|USD|EUR|GBP|SGD|CAD|AUD|CHF|SAR|QAR|OMR|KWD|BHD|INR|"
                    + "THB|MYR|HKD|JPY|CNY|NZD|PHP|IDR|VND|LKR|NPR|BDT)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CURRENCY_SYMBOL = Pattern.compile("[₹$€£]");

    private final CurrencyRatesProperties currencyRates;

    public SalaryRangeParser(CurrencyRatesProperties currencyRates) {
        this.currencyRates = currencyRates;
    }

    public Optional<Double> monthlyMidpointInr(String text) {
        return parse(text).map(SalaryRangeParseResult::monthlyMidInr);
    }

    public Optional<String> normalizeToInrLpa(String text) {
        return parse(text).map(SalaryRangeParseResult::normalizedLpaRange);
    }

    public Optional<SalaryRangeParseResult> parse(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        String normalized = text.trim().toLowerCase(Locale.ROOT)
                .replace(",", "")
                .replace("₹", "")
                .replace("inr", "")
                .trim();

        Matcher lpaRange = LPA_RANGE.matcher(normalized);
        if (lpaRange.find()) {
            double lowLpa = parseAmount(lpaRange.group(1));
            double highLpa = parseAmount(lpaRange.group(2));
            double annualLow = lowLpa * LPA_TO_ANNUAL_INR;
            double annualHigh = highLpa * LPA_TO_ANNUAL_INR;
            return Optional.of(toResult(annualLow, annualHigh, "INR"));
        }

        Matcher lpaSingle = LPA_SINGLE.matcher(normalized);
        if (lpaSingle.find()) {
            double lpa = parseAmount(lpaSingle.group(1));
            double annual = lpa * LPA_TO_ANNUAL_INR;
            return Optional.of(toResult(annual, annual, "INR"));
        }

        String currency = detectCurrency(text);
        if (isForeignCurrency(currency) && !currencyRates.isKnownCurrency(currency)) {
            return Optional.empty();
        }
        boolean monthly = isMonthly(text, currency);
        boolean annual = isAnnual(text, currency);

        Matcher numericRange = NUMERIC_RANGE.matcher(text);
        if (numericRange.find()) {
            double low = parseAmount(numericRange.group(1));
            double high = parseAmount(numericRange.group(2));
            return Optional.of(convertRange(currency, low, high, monthly, annual));
        }

        Matcher loneNumber = LONE_NUMBER.matcher(text);
        if (loneNumber.find()) {
            double value = parseAmount(loneNumber.group(1));
            if (value > 0) {
                return Optional.of(convertRange(currency, value, value, monthly, annual));
            }
        }
        return Optional.empty();
    }

    private SalaryRangeParseResult convertRange(String currency,
                                                double low,
                                                double high,
                                                boolean monthly,
                                                boolean annual) {
        double annualLowInr = toAnnualInr(currency, low, monthly, annual);
        double annualHighInr = toAnnualInr(currency, high, monthly, annual);
        return toResult(annualLowInr, annualHighInr, currency);
    }

    private SalaryRangeParseResult toResult(double annualLowInr, double annualHighInr, String currency) {
        double monthlyMid = annualToMonthly((annualLowInr + annualHighInr) / 2.0);
        return new SalaryRangeParseResult(
                monthlyMid,
                annualToMonthly(annualLowInr),
                annualToMonthly(annualHighInr),
                formatLpaRange(annualLowInr, annualHighInr),
                currency.toUpperCase(Locale.ROOT)
        );
    }

    private double toAnnualInr(String currency, double amount, boolean monthly, boolean annual) {
        double inrAmount = amount * currencyRates.rateFor(currency);
        if (monthly) {
            return inrAmount * 12.0;
        }
        if (annual || isForeignCurrency(currency) || inrAmount >= 300_000.0) {
            return inrAmount;
        }
        return inrAmount * 12.0;
    }

    private static boolean isForeignCurrency(String currency) {
        return currency != null && !"INR".equalsIgnoreCase(currency);
    }

    private static boolean isMonthly(String text, String currency) {
        return MONTHLY_HINT.matcher(text).find();
    }

    private static boolean isAnnual(String text, String currency) {
        if (ANNUAL_HINT.matcher(text).find()) {
            return true;
        }
        return isForeignCurrency(currency);
    }

    private static String detectCurrency(String text) {
        Matcher code = CURRENCY_CODE.matcher(text);
        if (code.find()) {
            return code.group(1).toUpperCase(Locale.ROOT);
        }
        Matcher genericForeign = Pattern.compile(
                "\\b([A-Z]{3})\\s+\\d", Pattern.CASE_INSENSITIVE).matcher(text);
        if (genericForeign.find()) {
            return genericForeign.group(1).toUpperCase(Locale.ROOT);
        }
        if (text.contains("₹")) {
            return "INR";
        }
        if (CURRENCY_SYMBOL.matcher(text).find()) {
            if (text.contains("$")) {
                return "USD";
            }
            if (text.contains("€")) {
                return "EUR";
            }
            if (text.contains("£")) {
                return "GBP";
            }
        }
        return "INR";
    }

    static String formatLpaRange(double annualLowInr, double annualHighInr) {
        long lpaLow = Math.round(annualLowInr / LPA_TO_ANNUAL_INR);
        long lpaHigh = Math.round(annualHighInr / LPA_TO_ANNUAL_INR);
        if (lpaLow == lpaHigh) {
            return String.format(Locale.ROOT, "₹%d LPA", lpaLow);
        }
        return String.format(Locale.ROOT, "₹%d-%d LPA", lpaLow, lpaHigh);
    }

    private static double parseAmount(String raw) {
        return Double.parseDouble(raw.replace(",", ""));
    }

    private static double annualToMonthly(double annualInr) {
        return annualInr / 12.0;
    }

    public record SalaryRangeParseResult(
            double monthlyMidInr,
            double monthlyLowInr,
            double monthlyHighInr,
            String normalizedLpaRange,
            String sourceCurrency
    ) {
    }
}
