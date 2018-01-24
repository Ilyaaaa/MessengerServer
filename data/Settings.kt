package data

import main.outInfoMessage
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.*

class Settings {
    private val DIR_NAME = "settings"
    private val CONF_NAME = DIR_NAME + "/server.ini"

    private var serverProps: Properties = Properties()

    init {
        try {
            File(DIR_NAME).mkdir()
            File(CONF_NAME).createNewFile()
            val fileReader = FileReader(CONF_NAME)

            serverProps.load(fileReader)
            fileReader.close()

            val property = serverProps.getProperty("port")
            if(property != null)
                port = property.toInt()

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun setPort(port: Int){
        Companion.port = port
        serverProps.setProperty("port", port.toString())
        saveSettings()
    }

    private fun saveSettings() {
        try {
            val dir = File(DIR_NAME)
            if (!dir.exists())
                dir.mkdir()

            val file = File(CONF_NAME)
            if (!file.exists())
                file.createNewFile()

            val confWriter = FileWriter(file.absoluteFile)
            serverProps.store(confWriter, null)
            confWriter.close()
            outInfoMessage("Settings saved")
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    companion object {
        var port = 4000
    }
}
