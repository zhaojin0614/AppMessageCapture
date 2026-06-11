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