package com.example.contactmanager.ui.contacts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.contactmanager.R

abstract class SwipeToDeleteCallback(context: Context) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val deleteIcon: Drawable? = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete)
    private val editIcon: Drawable? = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_edit)
    private val deleteColor: Int = Color.parseColor("#FF1744")
    private val editColor: Int = Color.parseColor("#2196F3")
    private val paint = Paint()

    private val intrinsicWidthDelete = deleteIcon?.intrinsicWidth ?: 0
    private val intrinsicHeightDelete = deleteIcon?.intrinsicHeight ?: 0
    private val intrinsicWidthEdit = editIcon?.intrinsicWidth ?: 0
    private val intrinsicHeightEdit = editIcon?.intrinsicHeight ?: 0

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        when (direction) {
            ItemTouchHelper.LEFT -> onLeftSwiped(viewHolder, direction)
            ItemTouchHelper.RIGHT -> onRightSwiped(viewHolder, position)
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top

        if (dX == 0f && !isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        when {
            dX > 0 -> {
                // Свайп вправо - редактирование
                paint.color = editColor
                c.drawRect(
                    itemView.left.toFloat(),
                    itemView.top.toFloat(),
                    itemView.left + dX,
                    itemView.bottom.toFloat(),
                    paint
                )

                editIcon?.let {
                    val iconTop = itemView.top + (itemHeight - intrinsicHeightEdit) / 2
                    val iconMargin = (itemHeight - intrinsicHeightEdit) / 2
                    val iconLeft = itemView.left + iconMargin
                    val iconRight = itemView.left + iconMargin + intrinsicWidthEdit
                    val iconBottom = iconTop + intrinsicHeightEdit

                    it.setBounds(iconLeft.toInt(), iconTop.toInt(), iconRight.toInt(), iconBottom.toInt())
                    it.draw(c)
                }
            }
            dX < 0 -> {
                // Свайп влево - удаление
                paint.color = deleteColor
                c.drawRect(
                    itemView.right + dX,
                    itemView.top.toFloat(),
                    itemView.right.toFloat(),
                    itemView.bottom.toFloat(),
                    paint
                )

                deleteIcon?.let {
                    val iconTop = itemView.top + (itemHeight - intrinsicHeightDelete) / 2
                    val iconMargin = (itemHeight - intrinsicHeightDelete) / 2
                    val iconLeft = itemView.right - iconMargin - intrinsicWidthDelete
                    val iconRight = itemView.right - iconMargin
                    val iconBottom = iconTop + intrinsicHeightDelete

                    it.setBounds(iconLeft.toInt(), iconTop.toInt(), iconRight.toInt(), iconBottom.toInt())
                    it.draw(c)
                }
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    fun closeSwipe(recyclerView: RecyclerView, position: Int) {
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
        viewHolder?.let {
            // Анимируем закрытие свайпа - сбрасываем положение
            recyclerView.animate().cancel()
            // Принудительно перерисовываем элемент
            it.itemView.invalidate()
            // Уведомляем адаптер о необходимости обновить элемент
            recyclerView.adapter?.notifyItemChanged(position)
        }
    }

    abstract fun onLeftSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int)
    abstract fun onRightSwiped(viewHolder: RecyclerView.ViewHolder, position: Int)
}