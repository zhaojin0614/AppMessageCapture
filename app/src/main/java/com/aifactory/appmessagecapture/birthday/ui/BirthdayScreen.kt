package com.aifactory.appmessagecapture.birthday.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aifactory.appmessagecapture.birthday.ui.BirthdayViewModel.BirthdayRoute

/**
 * BirthdayKeeper 模块的根 Composable。
 *
 * 内部通过 [BirthdayRoute] 状态机管理列表页与编辑页的切换，
 * 无需引入额外的 Navigation Compose 依赖。
 */
@Composable
fun BirthdayScreen(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    viewModel: BirthdayViewModel = viewModel()
) {
    val route by viewModel.route.collectAsState()

    when (val current = route) {
        is BirthdayRoute.List -> {
            BirthdayListScreen(
                modifier = modifier,
                viewModel = viewModel,
                onAddClick = { viewModel.navigateTo(BirthdayRoute.Edit(null)) },
                onEditClick = { id -> viewModel.navigateTo(BirthdayRoute.Edit(id)) }
            )
        }
        is BirthdayRoute.Edit -> {
            BirthdayEditScreen(
                modifier = modifier,
                viewModel = viewModel,
                birthdayId = current.birthdayId,
                onNavigateBack = { viewModel.navigateBack() }
            )
        }
    }
}
