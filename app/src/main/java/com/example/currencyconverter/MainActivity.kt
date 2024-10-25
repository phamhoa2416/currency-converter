package com.example.currencyconverter

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class MainActivity : AppCompatActivity() {
    private var baseCurrency = "EUR"
    private var convertedCurrency = "USD"
    private var currencyRates = mutableMapOf<String, Double>()

    private lateinit var fromCurrency: EditText
    private lateinit var toCurrency: EditText

    private var isUpdating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        fromCurrency = findViewById(R.id.fromCurrency)
        toCurrency = findViewById(R.id.toCurrency)

        spinnerSetup()
        setupTextWatchers()
        fetchExchangeRates()
    }

    private fun setupTextWatchers() {
        fromCurrency.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!isUpdating && s?.isNotEmpty() == true) {
                    try {
                        isUpdating = true
                        convertFromBaseCurrency()
                    } catch (e: Exception) {
                        Log.e("Main", "Error converting from base: $e")
                    } finally {
                        isUpdating = false
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        toCurrency.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!isUpdating && s?.isNotEmpty() == true) {
                    try {
                        isUpdating = true
                        convertToBaseCurrency()
                    } catch (e: Exception) {
                        Log.e("Main", "Error converting to base: $e")
                    } finally {
                        isUpdating = false
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    @SuppressLint("DefaultLocale")
    private fun convertFromBaseCurrency() {
        if (baseCurrency == convertedCurrency) {
            toCurrency.setText(fromCurrency.text.toString())
            return
        }

        try {
            val input = fromCurrency.text.toString().toDouble()
            val baseRate = currencyRates[baseCurrency] ?: return
            val targetRate = currencyRates[convertedCurrency] ?: return

            val result = (targetRate / baseRate) * input
            toCurrency.setText(String.format("%.2f", result))
        } catch (e: Exception) {
            Log.e("Conversion", "Error converting from base: ${e.message}")
        }
    }

    @SuppressLint("DefaultLocale")
    private fun convertToBaseCurrency() {
        if (baseCurrency == convertedCurrency) {
            fromCurrency.setText(toCurrency.text.toString())
            return
        }

        try {
            val input = toCurrency.text.toString().toDouble()
            val baseRate = currencyRates[baseCurrency] ?: return
            val targetRate = currencyRates[convertedCurrency] ?: return

            val result = (baseRate / targetRate) * input
            fromCurrency.setText(String.format("%.2f", result))
        } catch (e: Exception) {
            Log.e("Conversion", "Error converting to base: ${e.message}")
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun fetchExchangeRates() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val API = "https://api.exchangeratesapi.io/v1/latest?access_key=226b950af2928928506feb98c3d001d3"

                val response = URL(API).readText()
                val jsonObject = JSONObject(response)

                if (!jsonObject.getBoolean("success")) {
                    val error = jsonObject.getJSONObject("error")
                    throw Exception(error.getString("type") + ": " + error.getString("error"))
                }

                val rates = jsonObject.getJSONObject("rates")
                currencyRates.clear()

                currencyRates["EUR"] = 1.0
                val iterator = rates.keys()
                while (iterator.hasNext()) {
                    val currency = iterator.next()
                    currencyRates[currency] = rates.getDouble(currency)
                }

                withContext(Dispatchers.Main) {
                    if (fromCurrency.text.isNotEmpty()) {
                        convertFromBaseCurrency()
                    } else if (toCurrency.text.isNotEmpty()) {
                        convertToBaseCurrency()
                    }
                }
            } catch (e: Exception) {
                Log.e("API", "Error fetching rate: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        "Error fetching exchange rates: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun spinnerSetup() {
        val fromCurrencySpinner: Spinner = findViewById(R.id.fromCurrencySpinner)
        val toCurrencySpinner: Spinner = findViewById(R.id.toCurrencySpinner)

        ArrayAdapter.createFromResource(
            this,
            R.array.currencies,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            fromCurrencySpinner.adapter = adapter
            toCurrencySpinner.adapter = adapter
        }

        fromCurrencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                baseCurrency = parent?.getItemAtPosition(position).toString()
                if (fromCurrency.text.isNotEmpty()) {
                    convertFromBaseCurrency()
                } else if (toCurrency.text.isNotEmpty()) {
                    convertToBaseCurrency()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        toCurrencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                convertedCurrency = parent?.getItemAtPosition(position).toString()
                if (fromCurrency.text.isNotEmpty()) {
                    convertFromBaseCurrency()
                } else if (toCurrency.text.isNotEmpty()) {
                    convertToBaseCurrency()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}