package com.kuaida.core.data.repository

import com.kuaida.core.model.TemplateItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class TemplateRepository @Inject constructor() {
    private val templates = MutableStateFlow(
        listOf(
            TemplateItem(
                id = "intro",
                category = "通用 HR",
                title = "自我介绍",
                description = "结构化讲过去、现在、未来，并与目标岗位对齐。",
                prompt = "请帮我生成一段 90 秒左右的自我介绍，按过去、现在、未来结构展开。",
                readingMinutes = 2,
                favorite = false,
            ),
            TemplateItem(
                id = "why-us",
                category = "通用 HR",
                title = "为什么加入我们？",
                description = "把公司愿景、岗位价值和个人动机串联起来。",
                prompt = "请帮我回答‘为什么加入我们’，要求突出业务理解、个人动机和岗位匹配。",
                readingMinutes = 3,
                favorite = true,
            ),
            TemplateItem(
                id = "frontend",
                category = "技术（前端）",
                title = "系统设计：信息流",
                description = "聚焦状态管理、无限滚动虚拟化和乐观 UI 更新。",
                prompt = "请用面试口述方式，回答如何设计一个高性能信息流系统，重点说状态管理、缓存、虚拟列表和交互一致性。",
                readingMinutes = 10,
                favorite = false,
            ),
            TemplateItem(
                id = "metric",
                category = "产品案例",
                title = "提升某项指标",
                description = "使用 CIRCLES 风格解释指标诊断与策略优先级。",
                prompt = "请回答如何提升某个关键业务指标，要求包含问题拆解、假设、优先级和验证方案。",
                readingMinutes = 5,
                favorite = false,
            ),
        ),
    )

    fun observeTemplates(filter: String): Flow<List<TemplateItem>> {
        return templates.map { list ->
            list.filter {
                filter.isBlank() ||
                    it.title.contains(filter, true) ||
                    it.category.contains(filter, true) ||
                    it.description.contains(filter, true)
            }
        }
    }

    suspend fun toggleFavorite(id: String) {
        templates.value = templates.value.map { item ->
            if (item.id == id) item.copy(favorite = !item.favorite) else item
        }
    }
}
