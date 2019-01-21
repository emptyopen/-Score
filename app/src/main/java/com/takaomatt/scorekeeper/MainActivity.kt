package com.takaomatt.scorekeeper

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.app.Activity
import android.speech.RecognizerIntent
import android.content.Intent
import android.content.ActivityNotFoundException
import android.content.res.Configuration
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.*


// todo:
// add touch capability for entries and headers
// add by name
// remove by name?

class MainActivity : AppCompatActivity() {

    private val originalTeamNames = arrayOf("matt", "dolphins", "panthers", "rangers", "tigers", "stars", "wildcats", "bobcats", "magic", "jazz")
    private var txtSpeechInput: TextView? = null
    private val reqCodeSpeechInput = 100
    private val maxPlayers = 10

    private var sanitizedCommand = ""
    private var numPlayers = 1
    private var scoreTable = Array(11) {Stack<Int>()}
    private var teamNames = originalTeamNames
    private var log = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val actionBar = supportActionBar
        actionBar!!.hide()

        if (savedInstanceState != null) {
            scoreTable = savedInstanceState.getSerializable("scoreTable") as Array<Stack<Int>>
        }

        // TEXT SPEECH
        txtSpeechInput = findViewById(R.id.txtSpeechInput)
        val btnSpeak: ImageButton = findViewById(R.id.btnSpeak)
        btnSpeak.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                promptSpeechInput()
            }
        })

        val btnInfo: ImageButton = findViewById(R.id.btnInfo)
        btnInfo.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View) {
                toggleInfoWindow()
            }
        })

        drawTable()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("scoreTable", scoreTable)
        outState.putString("log", log)
        outState.putSerializable("teamNames", teamNames)
        outState.putInt("numPlayers", numPlayers)
        outState.putString("txtSpeech", sanitizedCommand)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState != null) {
            scoreTable = savedInstanceState.getSerializable("scoreTable") as Array<Stack<Int>>
            log = savedInstanceState.getString("log")!!
            teamNames = savedInstanceState.getSerializable("teamNames") as Array<String>
            numPlayers = savedInstanceState.getInt("numPlayers")
            sanitizedCommand = savedInstanceState.getString("txtSpeech")!!
        }
    }

    /**
     * Showing google speech input dialog
     */
    private fun promptSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            getString(R.string.speech_prompt)
        )
        try {
            startActivityForResult(intent, reqCodeSpeechInput)
        } catch (a: ActivityNotFoundException) {
            Toast.makeText(
                applicationContext,
                getString(R.string.speech_not_supported),
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    /**
     * Receiving speech input
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            reqCodeSpeechInput -> {
                if (resultCode == Activity.RESULT_OK && null != data) {

                    val result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    sanitizedCommand = sanitizeCommands(result[0])
                    txtSpeechInput!!.text = sanitizedCommand
                    executeCommand(sanitizedCommand)
                }
            }
        }
    }

    /**
     * Draw the table
     */
    private fun drawTable() {

        // portrait value defaults
        var numRows = 17
        var numFaded = 7
        var fadeRatio = 0.1

        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            numRows = 7
            numFaded = 4
            fadeRatio = 0.3

            val txtSpeech: TextView = findViewById(R.id.txtSpeechInput)
            val txtSpeechParams = txtSpeech.layoutParams as ViewGroup.MarginLayoutParams
            txtSpeechParams.topMargin = 30

            val dataTable: TableLayout = findViewById(R.id.dataTable)
            val dataTableParams = dataTable.layoutParams as ViewGroup.MarginLayoutParams
            dataTableParams.topMargin = 120
        }

        // TABLE LAYOUT
        val tl = findViewById<TableLayout>(R.id.dataTable)
        tl.removeAllViews()
        val fontSize = 22.toFloat()

        // HEADERS
        val headers = TableRow(this)
        headers.gravity = Gravity.CENTER
        for (x in 0 until numPlayers) {
            val column = TextView(this)
            column.setTypeface(null, Typeface.BOLD)
            column.text = teamNames[x]
            column.textSize = fontSize
            column.gravity = Gravity.CENTER
            headers.addView(column)
        }
        tl.addView(headers, TableLayout.LayoutParams())

        // SUM
        val sums = TableRow(this)
        sums.gravity = Gravity.CENTER
        for (x in 0 until numPlayers) {
            val column = TextView(this)
            column.setTypeface(null, Typeface.BOLD)
            column.text = scoreTable[x].sum().toString()
            column.textSize = fontSize
            column.gravity = Gravity.CENTER
            sums.addView(column)
        }
        tl.addView(sums, TableLayout.LayoutParams())

        // THE REST
        for (x in 0..numRows) { // num rows
            val newRow = TableRow(this)
            newRow.gravity = Gravity.CENTER
            for (y in 0 until numPlayers) { // num columns
                val column = TextView(this)
                if (scoreTable[y].size > x) {
                    column.text = scoreTable[y][scoreTable[y].size - x - 1].toString()
                } else {
                    column.text = " "
                }
                column.textSize = fontSize
                column.gravity = Gravity.CENTER
                newRow.addView(column)
                if (x > numFaded) {
                    column.alpha = (1 - fadeRatio * (x - numFaded)).toFloat()
                }
            }
            tl.addView(newRow, TableLayout.LayoutParams())
        }
    }

    /**
     * Runs all commands
     */
    private fun executeCommand(sanitizedCommand: String) {
        val convertedCommand = runCommands(sanitizedCommand)
        val testingView: TextView = findViewById(R.id.testView)
        testingView.text = convertedCommand
        if (scoreTable[0].size > 1) {
            testingView.text = scoreTable[0][1].toString()
        }
        drawTable()
    }

    /**
     * Controls each command
     */
    private fun runCommands(sanitizedCommand: String): String {
        val sanitizedCommands = sanitizedCommand.split(" and ")
        val trimmedRawCommands = sanitizedCommands.map { it.trim() }
        var latestCommand = "addData"
        for (command in trimmedRawCommands) {
            if (command.trim().startsWith("change") && !("color" in command)) { // Change team name
                changeTeam(command)
                latestCommand = "changeTeam"
            } else if (command.startsWith("add") && ("player" in command || "team" in command)) {
                addTeam(command)
                latestCommand = "addTeam"
            } else if ((command.startsWith("delete") || command.startsWith("remove")) && ("team" in command || "player" in command)) { // Remove a column
                deleteTeam(command)
                latestCommand = "deleteTeam"
            } else if (command.matches("""(add|\+|plus).*""".toRegex())) {
                addData(command)
                latestCommand = "addData"
            } else if (command.matches("""(subtract|-|minus).*""".toRegex())) {
                subtractData(command)
                latestCommand = "subtractData"
            } else if (command.matches("""update.*""".toRegex())) {
                updateData(command)
                latestCommand = "updateData"
            } else if (command.startsWith("clear")) {
                clearData(command)
                latestCommand = "clearData"
            } else if (command == "reset names") {
                teamNames = originalTeamNames
                log = "reset names"
            } else if (command.startsWith("reset")) {
                resetTeam(command)
            } else if (command == "change color") {
                val possibleColors = listOf(R.drawable.gradient1, R.drawable.gradient2, R.drawable.gradient3, R.drawable.gradient4, R.drawable.gradient5, R.drawable.gradient6)
                val relativeLayout: RelativeLayout = findViewById(R.id.relativeLayout)
                relativeLayout.setBackgroundResource(possibleColors.random())
                log = "changed color :)"
            } else {
                when(latestCommand) {
                    "addData" -> addData(command)
                    "subtractData" -> subtractData(command)
                    "resetTeamName" -> resetTeam(command)
                    "addTeamWithName" -> addTeamWithName(command)
                }
            }
        }
        return log
    }

    private fun findTeam(teamName: String): Int {
        teamNames.forEachIndexed { i, element -> // this is being skipped
            println(element)
            if (element.toLowerCase() == teamName.toLowerCase()) {
                return i
            }
        }
        return -1
    }

    private fun changeTeam(command: String) {
        val splitCommand = command.removePrefix("change").replace("team", "").split(" to ", " with ")
        if (splitCommand.size > 1) {
            val originalTeamName = splitCommand[0].trim()
            val newTeamName = splitCommand[1].trim()
            val teamIndex = findTeam(originalTeamName)
            if (teamIndex >= 0) {
                teamNames[teamIndex] = newTeamName
                log = "changed $originalTeamName to $newTeamName"
            }
        }
    }

    private fun addData(command: String) {
        val splitCommand = command.removePrefix("add").removePrefix("plus").removePrefix("+").split(" to ", " for ")
        if (splitCommand.size > 1) {
            val addedScore = verifyNumber(splitCommand[0].trim())
            if (addedScore != "") {
                val teamName = splitCommand[1].trim()
                val teamIndex = findTeam(teamName)
                if (teamIndex >= 0) {
                    scoreTable[teamIndex].push(addedScore.toInt())
                    log = "added $addedScore to $teamName"
                }
            }
        }
    }

    private fun subtractData(command: String) {
        val splitCommand = command.removePrefix("subtract").removePrefix("minus").removePrefix("-").split(" to ", " for ", " from ")
        if (splitCommand.size > 1) {
            val addedScore = verifyNumber(splitCommand[0].trim())
            if (addedScore != "") {
                val teamName = splitCommand[1].trim()
                val teamIndex = findTeam(teamName)
                if (teamIndex >= 0) {
                    scoreTable[teamIndex].push(addedScore.toInt() * -1)
                    log = "subtracted $addedScore from $teamName"
                }
            }
        }
    }

    private fun updateData(command: String) {
        val splitCommand = command.removePrefix("update").split(" to ")
        if (splitCommand.size > 1) {
            val updatedScore = verifyNumber(splitCommand[1].trim())
            if (updatedScore != "") {
                val teamName = splitCommand[0].trim()
                val teamIndex = findTeam(teamName)
                if (teamIndex >= 0) {
                    scoreTable[teamIndex].pop()
                    scoreTable[teamIndex].push(updatedScore.toInt())
                    log = "updated $teamName latest to $updatedScore"
                }
            }
        }
    }

    private fun deleteTeam(command: String) {
        val number = verifyNumber(command.removePrefix("delete").replace("teams", "").replace("players","").trim())
        if (number == "") {
            if (numPlayers > 1) {
                numPlayers -= 1
                log = "deleted one team"
            } else {
                log = "cannot have zero players"
            }
        } else {
            if (numPlayers - number.toInt() >= 1) {
                numPlayers -= number.toInt()
                log = "deleted $number teams"
            } else {
                log = "cannot have zero or less players"
            }
        }
    }

    private fun addTeam(command: String) {
        val potentialNumber = command.removePrefix("add").replace("teams", "").replace("players","").trim()
//        if (potentialNumber.split(" ").size > 1) {
//            addTeamWithName(command)
//        } else {
        val number = verifyNumber(potentialNumber)
        if (number == "") {
            if (numPlayers < maxPlayers) {
                numPlayers += 1
                log = "added one team"
            }
        } else {
            if (numPlayers + number.toInt() <= maxPlayers) {
                numPlayers += number.toInt()
                log = "added $number teams"
            }
        }
    }

    private fun addTeamWithName(command: String) {
        val name = command.removePrefix("add").trim()
        if (name != "") {
            if (numPlayers < maxPlayers) {
                numPlayers += 1
                teamNames[numPlayers - 1] = name
                log = "added team $name"
            }
        }
    }

    private fun clearData(command: String) {
        val teamName = command.removePrefix("clear").trim()
        val teamIndex = findTeam(teamName)
        if (teamIndex >= 0) {
            scoreTable[teamIndex].clear()
            log = "cleared $teamName"
        }
        if (teamName in listOf("all", "everything", "everyone")) {
            scoreTable.forEach { it.clear() }
            log = "cleared everything"
        }
    }

    private fun resetTeam(command: String) {
        val teamNumberString = command.removePrefix("reset").replace(" team ", " ").replace(" player ", " ").trim()
        val teamNumber = verifyNumber(teamNumberString)
        if (teamNumber != "" && teamNumber.toInt() - 1 >= 0 && teamNumber.toInt() - 1 < numPlayers) {
            teamNames[teamNumber.toInt() - 1] = originalTeamNames[teamNumber.toInt() - 1]
            log = "reset team $teamNumber"
        }
    }

    /**
     * Show / hide info window
     */
    private fun toggleInfoWindow() {
        val infoView = findViewById<TextView>(R.id.informationView)
        if (infoView.visibility == View.VISIBLE) {
            infoView.visibility = View.INVISIBLE
        } else {
            infoView.visibility = View.VISIBLE
        }
    }

    private fun verifyNumber(string: String): String {
        val numeric = string.matches("-?\\d+(\\.\\d+)?".toRegex())
        if (numeric) {
            return string
        }
        when (string) {
            in listOf("one") -> return "1"
            in listOf("two") -> return "2"
            in listOf("three") -> return "3"
            in listOf("four", "for") -> return "4"
            in listOf("five") -> return "5"
            in listOf("six") -> return "6"
            in listOf("seven") -> return "7"
            in listOf("eight", "a date", "date") -> return "8"
            in listOf("nine") -> return "9"
            in listOf("ten") -> return "10"
        }
        return ""
    }

    private fun sanitizeCommands(string: String): String {
        val changeMap = mapOf(" mat " to " matt ", " maisy " to " mazy ", " tonight " to " to matt ", " that " to " matt ",
            " teens " to " teams ", " points " to "", " point " to "", " christina " to " cristina ", " format " to " for matt ")
        var updatedString = (" $string ").toLowerCase()
        changeMap.forEach {(k, v) -> updatedString = updatedString.replace(k, v)}
        if (updatedString.startsWith("at "))
            updatedString = updatedString.replace("at ", "add ")
        updatedString = updatedString.trim()
        return updatedString
    }
}
