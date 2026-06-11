package com.aifactory.appmessagecapture.ui

import org.junit.Assert.*
import org.junit.Test

class CategoriesTest {
    
    @Test
    fun `expense categories should have 12 items`() {
        assertEquals(12, ExpenseCategories.all.size)
    }
    
    @Test
    fun `income categories should have 8 items`() {
        assertEquals(8, IncomeCategories.all.size)
    }
    
    @Test
    fun `expense categories should not have duplicates`() {
        assertEquals(ExpenseCategories.all.size, ExpenseCategories.all.toSet().size)
    }
    
    @Test
    fun `income categories should not have duplicates`() {
        assertEquals(IncomeCategories.all.size, IncomeCategories.all.toSet().size)
    }
    
    @Test
    fun `all expense categories should be non-empty strings`() {
        ExpenseCategories.all.forEach { category ->
            assertTrue("Category should not be empty", category.isNotBlank())
        }
    }
    
    @Test
    fun `all income categories should be non-empty strings`() {
        IncomeCategories.all.forEach { category ->
            assertTrue("Category should not be empty", category.isNotBlank())
        }
    }
}
