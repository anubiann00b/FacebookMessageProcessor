package me.shreyasr.fbmp

import java.io._
import java.text.{DateFormat, DateFormatSymbols, SimpleDateFormat}
import java.util.{Date, Locale}

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Document


object Parser {

  def main(args: Array[String]) {
    val filename = "messages.htm"
    val me = "My Name"
    val person = "Other Name"

    val readableFile = new PrintWriter(new File(s"$person.txt"))
    val rawFile = new PrintWriter(new File(s"$person-raw.txt"))

    getMessages(JsoupBrowser().parseFile(filename), me, person, {
      val df = new SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' k:mma z", Locale.US)
      df.setDateFormatSymbols({
        val symbols = new DateFormatSymbols(Locale.getDefault)
        symbols.setAmPmStrings(Array[String]("am", "pm")); symbols
      }); df
    }).foreach(m => {
      readableFile.write(m.toString + "\n")
      rawFile.write(m.message.toLowerCase.filterNot(".?!<>".toSet) + "\n")
    })

    readableFile.close()
    rawFile.close()
    // cat {NAME}-raw.txt | tr [:space:] '\n' | grep -v "^\s*$" | sort | uniq -c | sort -bnr >> {NAME}-wordlist.txt
  }

  def getMessages(document: Document, me: String, person: String, df: DateFormat): List[Message] = {
    val p = { var i = 0; () => { i += 1; i} }
    (document >> elementList(".thread"))
      .map(t => (t, t.innerHtml.slice(0, t.innerHtml.indexOf('\n')).split(", ")))
      .filter { case (_, names) => names.size == 2 && names.contains(me) && names.contains(person) }
      .map { case (thread, names) => thread }
      .map(thread => {
        val dates = (thread >> elementList(".message")).map(m => (m >> element(".meta")).text).map(df.parse)
        val senders = (thread >> elementList(".message")).map(m => (m >> element(".user")).text)
        val messages = (thread >> elementList("p")).map(_.innerHtml)
        zip3(dates, senders, messages)
      }).filter(_.forall({
        case (date, sender, message) => sender == me || sender == person
      })).flatten.map {
        case (date, sender, message) => new Message(date, sender, message)
      }.reverse.sorted
  }

  def zip3[A, B, C](l1 : List[A], l2 : List[B],l3 : List[C]): List[(A, B, C)] =
    l1.zip(l2).zip(l3).map { case ((a, b), c) => (a, b, c) }

  class Message(val date: Date, val sender: String, val message: String) extends Ordered[Message] {

    override def compare(that: Message): Int = this.date.compareTo(that.date)

    override def toString: String =
      s"${Message.renderDateFormat.format(date)} ${sender.substring(0, sender.indexOf(" "))}: $message"
  }

  object Message {
    val renderDateFormat: DateFormat = new SimpleDateFormat("MM/dd/yy hh:mm")
  }
}
