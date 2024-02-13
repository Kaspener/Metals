package ru.kaspenium.metalswidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
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


class MetalWidget : AppWidgetProvider() {
    val date1 = "13/02/2024"
    var resultDate = "13/02/2024"
    var resultBuy = ""
    var resultSell = ""
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        SendToBase()
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.metal_w_idget)
            views.setTextViewText(R.id.date, resultDate)
            views.setTextViewText(R.id.buyPrice, resultBuy)
            views.setTextViewText(R.id.sellPrice, resultSell)
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onEnabled(context: Context) {
    }

    override fun onDisabled(context: Context) {
    }

    fun SendToBase() {
        Thread {
            val cal = Calendar.getInstance()
            val currentTime: Date = cal.time
            val dateFormat: DateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date2 = dateFormat.format(currentTime)
            var url: URL
            var line = ""
            var connection: HttpURLConnection? = null
            try {
                url =
                    URL("http://www.cbr.ru/scripts/xml_metall.asp?date_req1=$date1&date_req2=$date2")
                connection = url.openConnection() as HttpURLConnection
                val br = BufferedReader(
                    InputStreamReader(
                        connection.inputStream
                    )
                )
                line = br.readLine()
                Log.d("HTTP-GET", line)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                connection?.disconnect()
            }
            val LOG_TAG = "myLogs"
            try {
                val factory = XmlPullParserFactory
                    .newInstance()
                factory.isNamespaceAware = true
                val xpp = factory.newPullParser()
                xpp.setInput(StringReader(line))
                var buy:String? = ""
                var sell:String? = ""
                var type = 0
                var state = 0
                while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
                    when (xpp.eventType) {
                        XmlPullParser.START_DOCUMENT -> Log.d(
                            LOG_TAG,
                            "START_DOCUMENT"
                        )

                        XmlPullParser.START_TAG -> {
                            if (xpp.name == "Record") {
                                state = 1
                                resultDate = xpp.getAttributeValue(0)
                                type = xpp.getAttributeValue(1).toInt()
                            } else if (xpp.name == "Buy") {
                                state = 2
                            } else if (xpp.name == "Sell") {
                                state = 3
                            }
                        }

                        XmlPullParser.END_TAG -> if (xpp.name == "Record") {
                            if (type == 1){
                                if (buy != null) {
                                    resultBuy = buy
                                }
                                if (sell != null) {
                                    resultSell = sell
                                }
                            }
                            buy = ""
                            sell = ""
                        }

                        XmlPullParser.TEXT -> {
                            if (state == 2) buy = xpp.text else if (state == 3) sell = xpp.text
                        }

                        else -> {}
                    }
                    // следующий элемент
                    xpp.next()
                }
            } catch (e: XmlPullParserException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }
}