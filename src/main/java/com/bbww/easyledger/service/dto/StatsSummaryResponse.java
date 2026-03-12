package com.bbww.easyledger.service.dto;

import java.math.BigDecimal;
import java.util.List;

public class StatsSummaryResponse {

    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private List<CategoryAmountItem> categoryTotals;
    private List<DailyAmountItem> dailyTotals;

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
}

