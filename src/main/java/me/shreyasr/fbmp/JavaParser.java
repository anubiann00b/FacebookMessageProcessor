package me.shreyasr.fbmp;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public class JavaParser {

    public static void main(String[] args) throws IOException, ParseException {
        SimpleDateFormat df = new SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' k:mma z", Locale.US);
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.getDefault());
        symbols.setAmPmStrings(new String[] { "am", "pm" });
        df.setDateFormatSymbols(symbols);

        String filename = "messages.htm";
        String person = "Person Name";

        Document messageDocument = Jsoup.parse(new File(filename), "UTF-8");
        Elements messageThreads = messageDocument.getElementsByClass("thread");

        Element conversationThread = null;
        for(Element thread : messageThreads) {
            String innerHtml = thread.html();
            String p1 = innerHtml.substring(0, innerHtml.indexOf(","));
            String p2 = innerHtml.substring(innerHtml.indexOf(",")+1, innerHtml.indexOf("\n"));
            if(p1.equals(person) || p2.equals(person)) {
                conversationThread = thread;
                break;
            }
        }

        if(conversationThread == null) {
            System.out.println("Person not found");
            return;
        }

        Elements headers = conversationThread.getElementsByClass("message");
        Elements messages = conversationThread.getElementsByTag("p");

        Iterator<Element> headersItr = headers.iterator();
        Iterator<Element> messagesItr = messages.iterator();

        for (; headersItr.hasNext(); ) {
            Element header = headersItr.next();
            Element messageElement = messagesItr.next();

            String sender = header.getElementsByClass("user").get(0).text();
            Date date = df.parse(header.getElementsByClass("meta").get(0).text());
            String messageText = messageElement.text();

            Message message = new Message(sender, date, messageText);
            System.out.println(message);
        }
    }

    private static class Message {

        private static DateFormat renderDateFormat = new SimpleDateFormat("MM/dd/yy hh:mm");

        private final String sender;
        private final Date date;
        private final String message;

        Message(String sender, Date date, String message) {
            this.sender = sender;
            this.date = date;
            this.message = message;
        }

        @Override
        public String toString() {
            return renderDateFormat.format(date)+ " " + sender.substring(0, sender.indexOf(" "))
                    + ": " + message;
        }
    }
}

