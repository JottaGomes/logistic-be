package com.adrianojlt.logistics.entity;

/**
 * Both kinds count towards total income, as described in the Calculate Profit
 * use case: "Sum of all customer payments and any additional income from agents".
 */
public enum IncomeType {
    CUSTOMER_PAYMENT,
    AGENT_INCOME
}
