package utb.fai;

public class App {

    public static void main(String[] args) {
        // args[0] - Adresa SMTP serveru (String)
        // args[1] - Číslo portu SMTP serveru (int)
        // args[2] - Email odesílatele (String)
        // args[3] - Email příjemce (String)
        // args[4] - Předmět emailu (String)
        // args[5] - Obsah emailu (String)

        if (args.length < 6) {
            System.err.println("Chyba: Nedostatecny pocet argumentu.");
            System.err.println("Pouziti: java -jar app.jar <host> <port> <from> <to> <subject> <text>");
            return;
        }

        String host = args[0];
        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Chyba: Neplatne cislo portu: " + args[1]);
            return;
        }

        String from = args[2];
        String to = args[3];
        String subject = args[4];

        // glue text together if text had spaces without quotes
        StringBuilder textBuilder = new StringBuilder();
        for (int i = 5; i < args.length; i++) {
            if (i > 5) {
                textBuilder.append(" ");
            }
            textBuilder.append(args[i]);
        }
        String text = textBuilder.toString();

        if (host.isEmpty() || from.isEmpty() || to.isEmpty()) {
            System.err.println("Chyba: Host, odesilatel ani prijemce nesmi byt prazdne.");
            return;
        }

        // connect and send
        EmailSender sender = null;
        try {
            sender = new EmailSender(host, port);
            sender.send(from, to, subject, text);
            System.out.println("Email byl uspesne odeslan!");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (sender != null) {
                sender.close();
            }
        }
    }
}
