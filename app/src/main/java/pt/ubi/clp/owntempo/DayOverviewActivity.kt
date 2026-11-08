package pt.ubi.clp.owntempo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import pt.ubi.clp.owntempo.ui.ConfirmDialog
import pt.ubi.clp.owntempo.ui.DayDialog
import pt.ubi.clp.owntempo.ui.DayOverviewAdapter
import pt.ubi.clp.owntempo.ui.TrainingViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class DayOverviewActivity : AppCompatActivity() {

    private val viewModel: TrainingViewModel by viewModels()
    private lateinit var adapter: DayOverviewAdapter
    private lateinit var layoutEmpty: View
    private lateinit var buttonAddAnotherTraining: Button
    private var dateEpoch: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day_overview)

        dateEpoch = intent.getLongExtra(EXTRA_DATE_EPOCH, -1L)
        if (dateEpoch == -1L) {
            finish()
            return
        }

        val date = LocalDate.ofEpochDay(dateEpoch)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = date
            .format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.getDefault()))
            .replaceFirstChar { it.uppercase() }

        toolbar.inflateMenu(R.menu.menu_day_overview)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_add_training) {
                addNewWorkout(date)
                true
            } else {
                false
            }
        }

        layoutEmpty = findViewById(R.id.layoutEmpty)
        buttonAddAnotherTraining = findViewById(R.id.buttonAddAnotherTraining)

        findViewById<Button>(R.id.buttonAddFirstTraining).setOnClickListener {
            addNewWorkout(date)
        }

        buttonAddAnotherTraining.setOnClickListener {
            addNewWorkout(date)
        }

        adapter = DayOverviewAdapter(
            onClick = { training ->
                startActivity(DayDetailActivity.newIntent(this, training.id))
            },
            onDelete = { training ->
                ConfirmDialog.show(
                    this,
                    getString(R.string.confirm_delete_session_title),
                    getString(R.string.confirm_delete_session_message)
                ) {
                    viewModel.deleteDay(training)
                }
            }
        )

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerDayTrainings)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.getDaysForDate(dateEpoch).collect { trainings ->
                        adapter.submitList(trainings)
                        val hasTrainings = trainings.isNotEmpty()
                        layoutEmpty.visibility = if (hasTrainings) View.GONE else View.VISIBLE
                        buttonAddAnotherTraining.visibility = if (hasTrainings) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.errors.collect {
                        Toast.makeText(this@DayOverviewActivity, it, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun addNewWorkout(date: LocalDate) {
        lifecycleScope.launch {
            val suggestions = viewModel.loadSuggestions()
            DayDialog.show(this@DayOverviewActivity, date, null, suggestions) { input ->
                viewModel.createDay(input.toNewDay(date)) { newId ->
                    startActivity(DayDetailActivity.newIntent(this@DayOverviewActivity, newId))
                }
            }
        }
    }

    companion object {
        private const val EXTRA_DATE_EPOCH = "extra_date_epoch"

        fun newIntent(context: Context, dateEpoch: Long): Intent =
            Intent(context, DayOverviewActivity::class.java).putExtra(EXTRA_DATE_EPOCH, dateEpoch)
    }
}