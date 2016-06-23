package me.shreyasr.fbmp

import java.text.{DateFormat, DateFormatSymbols, SimpleDateFormat}
import java.util.{Date, Locale}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

object Parser {

  val renderDateFormat: DateFormat = new SimpleDateFormat("MM/dd/yy hh:mm")
  val parseDateFormat: SimpleDateFormat = {
    val df = new SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' k:mma z", Locale.US)
    df.setDateFormatSymbols({
      val symbols = new DateFormatSymbols(Locale.getDefault)
      symbols.setAmPmStrings(Array[String]("am", "pm")); symbols
    }); df
  }

  def main(args: Array[String]) {
    val filename = "messages.htm"
    val person = "Person Name"

    (JsoupBrowser().parseFile(filename) >> elementList(".thread")).find(thread => {
      val text = thread.innerHtml
      val p1 = text.slice(0, text.indexOf(","))
      val p2 = text.slice(text.indexOf(",")+2, text.indexOf("\n"))
      p1 == person || p2 == person
    }) match {
      case None => println("Person not found")
      case Some(thread) => thread.flatMap(thread => {
        val dates = (thread >> elementList(".message")).map(m => (m >> element(".meta")).text).map(parseDateFormat.parse)
        val senders = (thread >> elementList(".message")).map(m => (m >> element(".user")).text)
        val messages = (thread >> elementList("p")).map(_.innerHtml)
        zip3(dates, senders, messages).map {
          case(date, sender, message) => new Message(date, sender, message)
        }
      }).toList.reverse.foreach(println)
    }
  }

  def zip3[A, B, C](l1 : List[A], l2 : List[B],l3 : List[C]) : List[(A, B, C)] =
    l1.zip(l2).zip(l3).map { case ((a, b), c) => (a, b, c) }

  private class Message(val date: Date, val sender: String, val message: String) {
    override def toString: String =
      s"${renderDateFormat.format(date)} ${sender.substring(0, sender.indexOf(" "))}: $message"
  }
}
