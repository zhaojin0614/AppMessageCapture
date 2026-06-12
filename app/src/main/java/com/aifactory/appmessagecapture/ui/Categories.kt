package com.aifactory.appmessagecapture.ui

// 支出类别常量
object ExpenseCategories {
    const val FOOD = "餐饮美食"
    const val TRANSPORT = "交通出行"
    const val SHOPPING = "购物消费"
    const val ENTERTAINMENT = "休闲娱乐"
    const val LIVING = "居家生活"
    const val MEDICAL = "医疗健康"
    const val EDUCATION = "教育培训"
    const val SOCIAL = "人情往来"
    const val BEAUTY = "美容护肤"
    const val PET = "宠物"
    const val FINANCE = "金融保险"
    const val OTHER = "其他支出"

    val all = listOf(
        FOOD, TRANSPORT, SHOPPING, ENTERTAINMENT, LIVING,
        MEDICAL, EDUCATION, SOCIAL, BEAUTY, PET, FINANCE, OTHER
    )
}

// 收入类别常量
object IncomeCategories {
    const val SALARY = "工资薪金"
    const val PARTTIME = "兼职副业"
    const val INVESTMENT = "投资理财"
    const val RENTAL = "租金收入"
    const val REFUND = "退款返现"
    const val RED_PACKET = "红包转账"
    const val REIMBURSEMENT = "报销补贴"
    const val OTHER = "其他收入"

    val all = listOf(
        SALARY, PARTTIME, INVESTMENT, RENTAL, REFUND,
        RED_PACKET, REIMBURSEMENT, OTHER
    )
}

/**
 * Mapping from legacy short category names to current category names.
 * Used for database migration and runtime category normalization.
 */
object CategoryMigration {
    /** old name → new name mapping for all known legacy categories */
    val mapping: Map<String, String> = mapOf(
        // Expense categories
        "餐饮"     to ExpenseCategories.FOOD,           // 餐饮美食
        "交通"     to ExpenseCategories.TRANSPORT,      // 交通出行
        "购物"     to ExpenseCategories.SHOPPING,        // 购物消费
        "娱乐"     to ExpenseCategories.ENTERTAINMENT,   // 休闲娱乐
        "生活缴费" to ExpenseCategories.LIVING,          // 居家生活
        "医疗"     to ExpenseCategories.MEDICAL,         // 医疗健康
        "其他"     to ExpenseCategories.OTHER,           // 其他支出
        // Income categories
        "工资"     to IncomeCategories.SALARY,           // 工资薪金
        "退款"     to IncomeCategories.REFUND,           // 退款返现
        "红包"     to IncomeCategories.RED_PACKET,       // 红包转账
        "理财收益" to IncomeCategories.INVESTMENT,       // 投资理财
        "转账"     to IncomeCategories.OTHER,            // 其他收入
    )
}