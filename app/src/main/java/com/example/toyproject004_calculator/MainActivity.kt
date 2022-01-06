package com.example.toyproject004_calculator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.room.Room
import com.example.toyproject004_calculator.model.History
import org.w3c.dom.Text
import java.lang.NumberFormatException

class MainActivity : AppCompatActivity() {

    private val expressionTextView: TextView by lazy {
        findViewById<TextView>(R.id.expressionTextView)
    }

    private val resultTextView: TextView by lazy {
        findViewById<TextView>(R.id.resultTextView)
    }

    private val historyLayout: View by lazy{
        findViewById<View>(R.id.historyLayout)
    }

    private val historyLinearLayout: LinearLayout by lazy{
        findViewById<LinearLayout>(R.id.historyLinearLayout)
    }

    lateinit var db: AppDatabase
    private var isOperator = false
    private var hasOperator = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "historyDB"
        ).build()
    }

    fun buttonClicked(v: View) {
        when (v.id) {
            R.id.zeroButton -> numberButtonClicked("0")
            R.id.oneButton -> numberButtonClicked("1")
            R.id.twoButton -> numberButtonClicked("2")
            R.id.threeButton -> numberButtonClicked("3")
            R.id.fourButton -> numberButtonClicked("4")
            R.id.fiveButton -> numberButtonClicked("5")
            R.id.sixButton -> numberButtonClicked("6")
            R.id.sevenButton -> numberButtonClicked("7")
            R.id.eightButton -> numberButtonClicked("8")
            R.id.nineButton -> numberButtonClicked("9")
            R.id.plusButton -> operationButtonClicked("+")
            R.id.minusButton -> operationButtonClicked("-")
            R.id.multiButton -> operationButtonClicked("*")
            R.id.dividerdButton -> operationButtonClicked("/")
            R.id.moduleButton -> operationButtonClicked("%")
        }

    }

    private fun numberButtonClicked(number: String) {
        if (isOperator) {
            expressionTextView.append(" ")
        }
        isOperator = false
        val expressionText = expressionTextView.text.split(" ")
        if (expressionText.isNotEmpty() && expressionText.last().length >= 15) {
            Toast.makeText(this, "15자리 까지만 사용할 수 있습니다.", Toast.LENGTH_SHORT).show()
            return
        } else if (expressionText.last().isEmpty() && number == "0") {
            Toast.makeText(this, "0은 제일 앞에 올 수 없습니다..", Toast.LENGTH_SHORT).show()
            return
        }
        expressionTextView.append(number)

        resultTextView.text = calculateExpression()
    }

    private fun operationButtonClicked(operator: String) {
        if (expressionTextView.text.isEmpty()) {
            return
        }
        when {
            isOperator -> {
                val text = expressionTextView.text.toString()
                expressionTextView.text = text.dropLast(1) + operator
            }
            hasOperator -> {
                Toast.makeText(this, "연산자는 한번만 사용할 수 있습니다.", Toast.LENGTH_SHORT).show()
                return
            }
            else -> {
                expressionTextView.append(" $operator")
//                (" $operator"):isOperator,hasOperator 둘다 false일 경우 숫자만 입력하고 연산자가
//                한번도 들어오지 않은 경우 숫자를 입력하고 그 뒤에 연산자 앞에 띄어쓰기가  하나 필요해서
//                띄어쓰기를 하고 연산자를 붙이도록 하였음.
            }
        }

        val ssb = SpannableStringBuilder(expressionTextView.text)
        ssb.setSpan(
            ForegroundColorSpan(getColor(R.color.green)),
            expressionTextView.text.length - 1,
            expressionTextView.text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        expressionTextView.text = ssb

        isOperator = true
        hasOperator = true
    }

    fun resultbuttonClicked(v: View) {
        val expressionTexts = expressionTextView.text.split(" ")
        if (expressionTextView.text.isEmpty() || expressionTexts.size == 1) {
            return
        }
        if (expressionTexts.size != 3 && hasOperator) {
            Toast.makeText(this, "아직 완성되지 않은 수식입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (expressionTexts[0].isNumber().not() || expressionTexts[2].isNumber().not()) {
            Toast.makeText(this, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val expressionText = expressionTextView.text.toString()
        val resultText = calculateExpression()

        //TODO DB에 넣어주는 부분

        Thread(Runnable {
            db.historyDao().insertHistory(History(null,expressionText,resultText))
        }).start()

       resultTextView.text = ""
        expressionTextView.text = resultText
        isOperator = false
        hasOperator = false
    }

    private fun calculateExpression(): String {
        val expressionTexts = expressionTextView.text.split(" ")
        if (hasOperator.not() || expressionTexts.size != 3) {
            return ""
        } else if (expressionTexts[0].isNumber().not() || expressionTexts[2].isNumber().not()) {
            return ""
        }
        val exp1 = expressionTexts[0].toBigInteger()
        val exp2 = expressionTexts[2].toBigInteger()
        val op = expressionTexts[1]

        return when (op) {
            "+" -> (exp1 + exp2).toString()
            "-" -> (exp1 - exp2).toString()
            "*" -> (exp1 * exp2).toString()
            "/" -> (exp1 / exp2).toString()
            "%" -> (exp1 % exp2).toString()
            else -> ""
        }
    }

    fun clearButtonClicked(v: View) {
        expressionTextView.text = ""
        resultTextView.text = ""
        isOperator = false
        hasOperator = false
    }

    fun historyButtonClicked(v: View) {
        historyLayout.isVisible =true
        historyLinearLayout.removeAllViews()
        //TODO 디비,뷰에서 모든 기록 할당하기기
        Thread(Runnable {
            db.historyDao().getAll().reversed().forEach{
                runOnUiThread{
                    val historyView =LayoutInflater.from(this).inflate(R.layout.history_row, null, false)
                    historyView.findViewById<TextView>(R.id.expressionTextView).text = it.expression
                    historyView.findViewById<TextView>(R.id.resultTextView).text ="= ${it.result}"

                    historyLinearLayout.addView(historyView)
                }
            }
        }).start()

    }
    fun closeHistoryButtonClicked(v: View){
        historyLayout.isVisible = false
    }

    fun historyClearButtonClicked(v:View){
        //TODO 디비,뷰에서 모든 기록 삭제
        historyLinearLayout.removeAllViews()
        Thread(Runnable {
            db.historyDao().deleteAll()
        }).start()
    }



}

fun String.isNumber(): Boolean {
    return try {
        this.toBigInteger()
        true
    } catch (e: NumberFormatException) {
        false
    }
}