package com.planbvalidator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@ConfigurationProperties(prefix = "planb.currency")
public record CurrencyRatesProperties(Map<String, Double> ratesToInr) {

    public CurrencyRatesProperties {
        ratesToInr = ratesToInr == null ? defaultRates() : normalize(ratesToInr);
    }

    public double rateFor(String currencyCode) {
        return findRate(currencyCode).orElse(1.0);
    }

    public java.util.Optional<Double> findRate(String currencyCode) {
        String code = normalizeCode(currencyCode);
        Double rate = ratesToInr.get(code);
        return rate != null && rate > 0 ? java.util.Optional.of(rate) : java.util.Optional.empty();
    }

    public boolean isKnownCurrency(String currencyCode) {
        return findRate(currencyCode).isPresent();
    }

    private static Map<String, Double> normalize(Map<String, Double> source) {
        Map<String, Double> normalized = new LinkedHashMap<>(defaultRates());
        source.forEach((code, rate) -> {
            if (code != null && !code.isBlank() && rate != null && rate > 0) {
                normalized.put(normalizeCode(code), rate);
            }
        });
        return Map.copyOf(normalized);
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

  /** Approximate FX to INR; override via planb.currency.rates-to-inr. */
    private static Map<String, Double> defaultRates() {
        Map<String, Double> rates = new LinkedHashMap<>();
        rates.put("INR", 1.0);
        // Middle East
        rates.put("AED", 22.5);
        rates.put("SAR", 22.2);
        rates.put("QAR", 22.8);
        rates.put("OMR", 215.0);
        rates.put("KWD", 270.0);
        rates.put("BHD", 254.0);
        // Americas / Europe / Oceania
        rates.put("USD", 83.0);
        rates.put("EUR", 90.0);
        rates.put("GBP", 105.0);
        rates.put("CAD", 61.0);
        rates.put("AUD", 54.0);
        rates.put("NZD", 50.0);
        rates.put("CHF", 95.0);
        // Asia-Pacific
        rates.put("SGD", 62.0);
        rates.put("HKD", 10.6);
        rates.put("JPY", 0.55);
        rates.put("CNY", 11.5);
        rates.put("THB", 2.3);
        rates.put("MYR", 17.7);
        rates.put("PHP", 1.45);
        rates.put("IDR", 0.0052);
        rates.put("VND", 0.0033);
        // South Asia
        rates.put("LKR", 0.28);
        rates.put("NPR", 0.62);
        rates.put("BDT", 0.75);
        return rates;
    }
}
