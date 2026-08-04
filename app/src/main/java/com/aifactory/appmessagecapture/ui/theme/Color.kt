package com.aifactory.appmessagecapture.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Soft UI Evolution palette — mint teal x mist blue x warm sand
// Light + Dark, WCAG AA checked
// ============================================================

// Brand: mint teal
val BrandTeal = Color(0xFF0FA18D)
val BrandTealDark = Color(0xFF0B6E60)
val BrandTealLight = Color(0xFFCFF3EC)
val OnBrandTeal = Color(0xFFFFFFFF)

// Brand: mist blue
val MistBlue = Color(0xFF5C7FB8)
val MistBlueDark = Color(0xFF3D5A8A)
val MistBlueLight = Color(0xFFDFE8F6)
val OnMistBlue = Color(0xFFFFFFFF)

// Warm sand tertiary (birthday module)
val SandGold = Color(0xFFB98A5E)
val SandGoldDark = Color(0xFF8A6138)
val SandGoldLight = Color(0xFFF6EAD9)

// Light neutrals
val BackgroundLight = Color(0xFFF7F9F8)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFEDF2F0)
val OnBackgroundLight = Color(0xFF1B2C28)
val OnSurfaceLight = Color(0xFF24322E)
val OnSurfaceVariantLight = Color(0xFF66766F)
val OutlineLight = Color(0xFFD8E2DE)

// Dark neutrals
val BackgroundDark = Color(0xFF0F1614)
val SurfaceDark = Color(0xFF151D1B)
val SurfaceVariantDark = Color(0xFF232D2A)
val OnBackgroundDark = Color(0xFFDDE6E2)
val OnSurfaceDark = Color(0xFFD9E2DE)
val OnSurfaceVariantDark = Color(0xFF9DAEAA)
val OutlineDark = Color(0xFF3D4B47)

// Error
val ErrorRed = Color(0xFFD95050)
val ErrorRedDark = Color(0xFFF08787)
val OnErrorLight = Color(0xFFFFFFFF)
val OnErrorDark = Color(0xFF4A1616)
val ErrorContainerLight = Color(0xFFFDE7E7)
val ErrorContainerDark = Color(0xFF5C2626)
val OnErrorContainerLight = Color(0xFF7A2A2A)
val OnErrorContainerDark = Color(0xFFFFDADA)

// Semantic finance colors
val ExpenseRed = Color(0xFFE45A4F)
val ExpenseRedLight = Color(0xFFF2A29B)
val IncomeGreen = Color(0xFF3FAE7E)
val IncomeGreenLight = Color(0xFF86D4B4)

// Report chart color (mist blue replaces the old ReportBlue)
val ReportBlue = Color(0xFF5B9BD8)
val ReportBlueLight = Color(0xFF9FC4EC)
val ReportBlueBg = Color(0xFFEFF5FC)
val ReportCardBg = Color(0xFFFFFFFF)
val ReportBgGray = Color(0xFFF6F8F7)
val ReportTextGray = Color(0xFF8A9B96)
val ReportTextDark = Color(0xFF2A3833)
val ReportDivider = Color(0xFFE6EDEA)
val ReportNeutralGray = Color(0xFFE7EEEA)

// 支出类别颜色（12个大类）— softened palette
val CategoryFood = Color(0xFFF0A24A)        // 餐饮美食
val CategoryTransport = Color(0xFF55B28C)   // 交通出行
val CategoryShopping = Color(0xFF7D8FF2)    // 购物消费
val CategoryEntertainment = Color(0xFFEE7BA6) // 休闲娱乐
val CategoryLiving = Color(0xFF5BA8D8)      // 居家生活
val CategoryMedical = Color(0xFFA97BD6)     // 医疗健康
val CategoryEducation = Color(0xFFF2B84B)   // 教育培训
val CategorySocial = Color(0xFF7B7FD6)      // 人情往来
val CategoryBeauty = Color(0xFFEF8C8C)      // 美容护肤
val CategoryPet = Color(0xFF9B8CE8)         // 宠物
val CategoryFinance = Color(0xFF48B895)     // 金融保险
val CategoryUncategorized = Color(0xFF98A4A1) // 其他支出

// 收入类别颜色（8个大类）
val CategorySalary = Color(0xFF3FAE7E)      // 工资薪金
val CategoryParttime = Color(0xFF5B9BD8)    // 兼职副业
val CategoryInvestment = Color(0xFFF0A24A)  // 投资理财
val CategoryRental = Color(0xFFA97BD6)      // 租金收入
val CategoryRefund = Color(0xFF4CB5C0)      // 退款返现
val CategoryRedPacket = Color(0xFFEE7B6C)   // 红包转账
val CategoryReimbursement = Color(0xFFA58C6D) // 报销补贴
val CategoryOtherIncome = Color(0xFF7C8B98) // 其他收入

// 向后兼容别名（旧版分类名映射）
val CategoryBills = CategoryLiving

// Gradient stops for soft hero cards
val GradientExpenseStart = Color(0xFFEF7D6A)
val GradientExpenseEnd = Color(0xFFF2A29B)
val GradientIncomeStart = Color(0xFF43B98A)
val GradientIncomeEnd = Color(0xFF86D4B4)
val GradientBrandStart = Color(0xFF0FA18D)
val GradientBrandEnd = Color(0xFF5BC0B4)
val GradientBrandSoftStart = Color(0xFFCFF3EC)
val GradientBrandSoftEnd = Color(0xFFE9F8F5)
