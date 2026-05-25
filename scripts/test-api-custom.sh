#!/usr/bin/env bash
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
HR="━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

section() { echo; echo "$HR"; echo "▶ $1"; echo "$HR"; }

pretty() { python3 -m json.tool 2>/dev/null || cat; }

section "1. Health"
curl -s "$BASE/v1/health" | pretty

section "2. Questionnaire questions (first 2)"
curl -s "$BASE/v1/questionnaire/questions" | python3 -c "
import json,sys
d=json.load(sys.stdin)
print(json.dumps({'count': len(d.get('questions',[])), 'firstTwo': d.get('questions',[])[:2]}, indent=2))
"

section "3. Questionnaire score"
curl -s -X POST "$BASE/v1/questionnaire/score" \
  -H 'Content-Type: application/json' \
  -d '{
    "psychology": {
      "uncertaintyTolerance": 4,
      "discipline": 3,
      "stressRecovery": 4,
      "validationDependency": 2,
      "impulsiveness": 2,
      "routineAdherence": 4,
      "setbackRecovery": 4,
      "uncertaintyStamina": 3,
      "financialResilience": 4,
      "selfDirectedMotivation": 4
    }
  }' | pretty

section "4. Runway calculate"
curl -s -X POST "$BASE/v1/runway/calculate" \
  -H 'Content-Type: application/json' \
  -d '{"liquidSavings": 900000, "monthlyExpenses": 65000, "debtObligations": 0}' | pretty

section "5. Analyze — side hustle (keep job, research off)"
curl -s -X POST "$BASE/v1/analyze" \
  -H 'Content-Type: application/json' \
  -d '{
    "profile": {
      "currentProfession": "Backend Engineer",
      "industry": "Fintech",
      "yearsExperience": 4,
      "country": "India",
      "city": "Bengaluru"
    },
    "financials": {
      "monthlyIncome": 180000,
      "liquidSavings": 900000,
      "monthlyExpenses": 65000,
      "dependents": 0,
      "debtObligations": 0,
    },
    "planB": {
      "title": "AI Freelance Consulting",
      "description": "Evening/weekend AI consulting for startups",
      "reason": "Build independent income while keeping salary safety net",
      "timelineMonths": 9,
      "iWillQuitMyJob": false,
      "expectedIncome3Months": 15000,
      "expectedIncome6Months": 55000,
      "expectedIncome12Months": 120000
    },
    "constraints": {
      "successDefinition": "3 paying clients and ₹1L+/month side income",
      "biggestFear": "Burnout from dual workload",
      "acceptableDownside": "Pause Plan B if health suffers",
      "minimumAcceptableSalary": 100000,
      "acceptableMonthsWithoutIncome": 4,
      "familyPressureLevel": 2
    },
    "psychology": {
      "uncertaintyTolerance": 4,
      "discipline": 4,
      "stressRecovery": 3,
      "validationDependency": 2,
      "impulsiveness": 2,
      "routineAdherence": 4,
      "setbackRecovery": 4,
      "uncertaintyStamina": 4,
      "financialResilience": 4,
      "selfDirectedMotivation": 5
    },
    "researchOptions": { "enableResearch": false }
  }' | python3 -c "
import json,sys
r=json.load(sys.stdin)
keys=['requestId','overallVerdict','feasibilityScore','riskScore','confidence','runwayMonths',
      'scoreBreakdown','opportunityCost','aiProviders','dataGaps','recommendationSummary']
out={k:r.get(k) for k in keys if k in r}
print(json.dumps(out, indent=2))
"

section "6. Analyze — full-time leap (quit job, tight runway, research off)"
curl -s -X POST "$BASE/v1/analyze" \
  -H 'Content-Type: application/json' \
  -d '{
    "profile": {
      "currentProfession": "Product Manager",
      "industry": "SaaS",
      "yearsExperience": 7,
      "country": "India",
      "city": "Mumbai"
    },
    "financials": {
      "monthlyIncome": 250000,
      "liquidSavings": 400000,
      "monthlyExpenses": 95000,
      "dependents": 1,
      "debtObligations": 200000,
    },
    "planB": {
      "title": "D2C Wellness Brand",
      "description": "Launch direct-to-consumer Ayurvedic supplements online",
      "reason": "Own a brand and escape corporate ceiling",
      "timelineMonths": 6,
      "iWillQuitMyJob": true,
      "expectedIncome3Months": 0,
      "expectedIncome6Months": 40000,
      "expectedIncome12Months": 150000
    },
    "constraints": {
      "successDefinition": "₹5L monthly revenue with positive unit economics",
      "biggestFear": "Running out of savings before product-market fit",
      "acceptableDownside": "Return to PM role within 12 months",
      "minimumAcceptableSalary": 180000,
      "acceptableMonthsWithoutIncome": 3,
      "familyPressureLevel": 4
    },
    "psychology": {
      "uncertaintyTolerance": 3,
      "discipline": 4,
      "stressRecovery": 3,
      "validationDependency": 3,
      "impulsiveness": 3,
      "routineAdherence": 3,
      "setbackRecovery": 3,
      "uncertaintyStamina": 3,
      "financialResilience": 2,
      "selfDirectedMotivation": 4
    },
    "researchOptions": { "enableResearch": false }
  }' | python3 -c "
import json,sys
r=json.load(sys.stdin)
keys=['requestId','overallVerdict','feasibilityScore','riskScore','confidence','runwayMonths',
      'scoreBreakdown','opportunityCost','aiProviders','dataGaps','recommendationSummary']
out={k:r.get(k) for k in keys if k in r}
print(json.dumps(out, indent=2))
"

section "7. Analyze JSON — side hustle payload (summary)"
curl -s -m 240 -X POST "$BASE/v1/analyze" \
  -H 'Content-Type: application/json' \
  -d '{
    "profile": {"currentProfession":"Data Analyst","industry":"E-commerce","yearsExperience":3,"country":"India","city":"Pune"},
    "financials": {"monthlyIncome":120000,"liquidSavings":600000,"monthlyExpenses":55000,"dependents":0,"debtObligations":50000},
    "planB": {"title":"Analytics Freelancing","description":"Shopify analytics for D2C brands","reason":"Flexibility","timelineMonths":6,"iWillQuitMyJob":false,"expectedIncome3Months":10000,"expectedIncome6Months":45000,"expectedIncome12Months":90000},
    "constraints": {"successDefinition":"2 retainer clients","biggestFear":"Client churn","acceptableDownside":"Stay employed","minimumAcceptableSalary":80000,"acceptableMonthsWithoutIncome":5,"familyPressureLevel":2},
    "psychology": {"uncertaintyTolerance":4,"discipline":3,"stressRecovery":4,"validationDependency":2,"impulsiveness":2,"routineAdherence":4,"setbackRecovery":4,"uncertaintyStamina":3,"financialResilience":4,"selfDirectedMotivation":4},
    "researchOptions": {"enableResearch": false}
  }' | python3 -c "import sys,json; r=json.load(sys.stdin); print(r.get('overallVerdict'), r.get('runwayMonths'))"

echo
echo "Done."
