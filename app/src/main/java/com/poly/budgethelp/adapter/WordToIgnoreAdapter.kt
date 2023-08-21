package com.poly.budgethelp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.poly.budgethelp.R
import com.poly.budgethelp.data.WordToIgnore

class WordToIgnoreAdapter : ListAdapter<WordToIgnore, WordToIgnoreAdapter.WordToIgnoreViewHolder>(WordToIgnoreComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordToIgnoreViewHolder {
        return WordToIgnoreViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: WordToIgnoreViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    class WordToIgnoreViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.wordToIgnoreTextView)

        fun bind(word: WordToIgnore) {
            textView.text = word.word
        }

        companion object {
            fun create(parent: ViewGroup): WordToIgnoreViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_word_to_ignore, parent, false)
                return WordToIgnoreViewHolder(view)
            }
        }
    }

    class WordToIgnoreComparator : DiffUtil.ItemCallback<WordToIgnore>() {
        override fun areItemsTheSame(oldItem: WordToIgnore, newItem: WordToIgnore): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: WordToIgnore, newItem: WordToIgnore): Boolean {
            return oldItem.word == newItem.word
        }
    }
}