package astah.plugin

import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi

// the left hand side of :: must not be generic types and thus need to prepare two converters without using generics
object JsonSaveDataConverter {
    private val moshiForTickets = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val moshiForInt = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val jsonAdapter1 = moshiForTickets.adapter(Array<Ticket>::class.java)
    private val jsonAdapter2 = moshiForInt.adapter(Array<ColumnWidth>::class.java)
    fun convertFromTicketsToJSON(models: Array<Ticket>): String = jsonAdapter1.toJson(models)
    fun convertFromJsonToTickets(json: String): MutableList<Ticket> = jsonAdapter1.fromJson(json)!!.toMutableList()
    fun convertFromWidthsToJSON(models: Array<ColumnWidth>): String = jsonAdapter2.toJson(models)
    fun convertFromJsonToWidths(json: String): MutableList<ColumnWidth> = jsonAdapter2.fromJson(json)!!.toMutableList()
}