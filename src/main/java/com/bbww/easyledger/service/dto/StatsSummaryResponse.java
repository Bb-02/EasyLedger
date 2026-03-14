package com.bbww.easyledger.service.dto;

import java.math.BigDecimal;
import java.util.List;

public class StatsSummaryResponse {

    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private List<CategoryAmountItem> categoryTotals;
    private List<DailyAmountItem> dailyTotals;
    private List<CategoryAmountItem> categoryTotalsIncome;
    private List<CategoryAmountItem> categoryTotalsExpense;
    private List<DailyAmountItem> dailyIncomeTotals;
    private List<DailyAmountItem> dailyExpenseTotals;
    private List<PeriodAmountItem> periodIncomeTotals;
    private List<PeriodAmountItem> periodExpenseTotals;
    private List<PeriodAmountItem> periodTotals;

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public List<CategoryAmountItem> getCategoryTotals() {
        return categoryTotals;
    }

    public void setCategoryTotals(List<CategoryAmountItem> categoryTotals) {
        this.categoryTotals = categoryTotals;
    }

    public List<DailyAmountItem> getDailyTotals() {
        return dailyTotals;
    }

    public void setDailyTotals(List<DailyAmountItem> dailyTotals) {
        this.dailyTotals = dailyTotals;
    }

    public List<CategoryAmountItem> getCategoryTotalsIncome() {
        return categoryTotalsIncome;
    }

    public void setCategoryTotalsIncome(List<CategoryAmountItem> categoryTotalsIncome) {
        this.categoryTotalsIncome = categoryTotalsIncome;
    }

    public List<CategoryAmountItem> getCategoryTotalsExpense() {
        return categoryTotalsExpense;
    }

    public void setCategoryTotalsExpense(List<CategoryAmountItem> categoryTotalsExpense) {
        this.categoryTotalsExpense = categoryTotalsExpense;
    }

    public List<DailyAmountItem> getDailyIncomeTotals() {
        return dailyIncomeTotals;
    }

    public void setDailyIncomeTotals(List<DailyAmountItem> dailyIncomeTotals) {
        this.dailyIncomeTotals = dailyIncomeTotals;
    }

    public List<DailyAmountItem> getDailyExpenseTotals() {
        return dailyExpenseTotals;
    }

    public void setDailyExpenseTotals(List<DailyAmountItem> dailyExpenseTotals) {
        this.dailyExpenseTotals = dailyExpenseTotals;
    }

    public List<PeriodAmountItem> getPeriodIncomeTotals() {
        return periodIncomeTotals;
    }

    public void setPeriodIncomeTotals(List<PeriodAmountItem> periodIncomeTotals) {
        this.periodIncomeTotals = periodIncomeTotals;
    }

    public List<PeriodAmountItem> getPeriodExpenseTotals() {
        return periodExpenseTotals;
    }

    public void setPeriodExpenseTotals(List<PeriodAmountItem> periodExpenseTotals) {
        this.periodExpenseTotals = periodExpenseTotals;
    }

    public List<PeriodAmountItem> getPeriodTotals() {
        return periodTotals;
    }

    public void setPeriodTotals(List<PeriodAmountItem> periodTotals) {
        this.periodTotals = periodTotals;
    }

    public static class CategoryAmountItem {
        private String categoryName;
        private BigDecimal amount;

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    public static class DailyAmountItem {
        private String day;
        private BigDecimal amount;

        public String getDay() {
            return day;
        }

        public void setDay(String day) {
            this.day = day;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    public static class PeriodAmountItem {
        private String label;
        private BigDecimal amount;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}
