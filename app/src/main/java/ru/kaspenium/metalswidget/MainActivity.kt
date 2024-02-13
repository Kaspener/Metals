package ru.kaspenium.metalswidget

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private var date1 = "13/02/2024"
    private var resultData = "13/02/2024"

    val metals = arrayOf(Record("", ""), Record("", ""), Record("", ""), Record("", ""))

    inner class Record(
        var sell: String?,
        var buy: String?
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getRecords()

    }

    fun getRecords() {
        Thread {
            val cal = Calendar.getInstance()
            val currentTime = cal.time
            val dateFormat: DateFormat =
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date2 = dateFormat.format(currentTime)
            var url: URL
            var line = ""
            var connection: HttpURLConnection? = null
            try {
                url =
                    URL("http://www.cbr.ru/scripts/xml_metall.asp?date_req1=$date1&date_req2=$date2")
                connection = url.openConnection() as HttpURLConnection
                val br = BufferedReader(InputStreamReader(connection.inputStream))
                line = br.readLine()
            } catch (e: IOException) {
                e.printStackTrace() //вывод метода который в данный момент выполняется
            } finally {
                connection?.disconnect()
            }
            try {
                val factory = XmlPullParserFactory
                    .newInstance()
                factory.isNamespaceAware = true
                val xpp = factory.newPullParser()
                xpp.setInput(StringReader(line))
                var type = 0
                var sell: String? = ""
                var buy: String? = ""
                var state = 0
                while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
                    when (xpp.eventType) {

                        XmlPullParser.START_TAG -> {
                            if (xpp.name == "Record") {
                                state = 1
                                resultData = xpp.getAttributeValue(0)
                                type = xpp.getAttributeValue(1).toInt()
                            } else if (xpp.name == "Buy") {
                                state = 2
                            } else if (xpp.name == "Sell") {
                                state = 3
                            }
                        }

                        XmlPullParser.END_TAG -> if (xpp.name == "Record") {
                            metals[type - 1].buy = buy
                            metals[type - 1].sell = sell
                            type = 0
                            buy = ""
                            sell = ""
                        }

                        XmlPullParser.TEXT -> {
                            if (state == 2) buy = xpp.text else if (state == 3) sell = xpp.text
                        }

                        else -> {}
                    }
                    xpp.next()
                }
            } catch (e: XmlPullParserException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                runOnUiThread {
                    val table = findViewById<TableLayout>(R.id.table)
                    val param = TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                    for (i in 0 until 4) {
                        val tableRow = TableRow(this)
                        tableRow.layoutParams = TableRow.LayoutParams(
                            TableRow.LayoutParams.MATCH_PARENT,
                            TableRow.LayoutParams.WRAP_CONTENT
                        )
                        val name = TextView(this)
                        val buy = TextView(this)
                        val sell = TextView(this)
                        name.layoutParams = param
                        name.gravity = Gravity.CENTER
                        name.text = when (i) {
                            0 -> "Золото"
                            1 -> "Серебро"
                            2 -> "Платина"
                            else -> "Паладий"
                        }
                        buy.layoutParams = param
                        buy.gravity = Gravity.CENTER
                        buy.text = metals[i].buy
                        sell.layoutParams = param
                        sell.gravity = Gravity.CENTER
                        sell.text = metals[i].sell
                        tableRow.addView(name)
                        tableRow.addView(buy)
                        tableRow.addView(sell)
                        table.addView(tableRow)
                    }
                    val data = findViewById<TextView>(R.id.dateMain)
                    data.text = resultData
                }
                Thread.sleep(300)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }.start()
    }
}