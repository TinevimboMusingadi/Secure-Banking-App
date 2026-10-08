package com.securebank.ui;

/**
 * Creative ASCII Art Banners and visual design components for the Secure Banking Console.
 * Designed for visual clarity, elegance, and distinct terminal branding.
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public final class AsciiArt {

    private AsciiArt() {}

    public static final String MUSINGADI_BANNER =
            "========================================================================================\n" +
            "   ____             _    _               __          ___ _   _     \n" +
            "  |  _ \\           | |  (_)              \\ \\        / (_) | | |    \n" +
            "  | |_) | __ _ _ __ | | ___ _ __   __ _    \\ \\  /\\  / / _| |_| |__  \n" +
            "  |  _ < / _` | '_ \\| |/ / | '_ \\ / _` |    \\ \\/  \\/ / | | __| '_ \\ \n" +
            "  | |_) | (_| | | | |   <| | | | | (_| |     \\  /\\  /  | | |_| | | |\n" +
            "  |____/ \\__,_|_| |_|_|\\_\\_|_| |_|\\__, |      \\/  \\/   |_|\\__|_| |_|\n" +
            "                                   __/ |                            \n" +
            "                                  |___/                             \n" +
            "   __  __ _    _  _____ _____ _   _  _____          _____ _____ \n" +
            "  |  \\/  | |  | |/ ____|_   _| \\ | |/ ____|   /\\   |  __ \\_   _|\n" +
            "  | \\  / | |  | | (___   | | |  \\| | |  __   /  \\  | |  | || |  \n" +
            "  | |\\/| | |  | |\\___ \\  | | | . ` | | |_ | / /\\ \\ | |  | || |  \n" +
            "  | |  | | |__| |____) |_| |_| |\\  | |__| |/ ____ \\| |__| || |_ \n" +
            "  |_|  |_|\\____/|_____/|_____|_| \\_|\\_____/_/    \\_\\_____/_____|\n" +
            "                                                                        \n" +
            "                       [ BANKING WITH MUSINGADI ]                       \n" +
            "               Tinevimbo Musingadi | ISA Reg: H250125B                  \n" +
            "               Zero-Trust * PBKDF2-HMAC * RBAC * Audit                  \n" +
            "========================================================================================";

    public static final String MAIN_BANNER = MUSINGADI_BANNER;

    public static final String VAULT_ICON =
            "                       .-----------------------------------.\n" +
            "                      /   .-----------------------------.   \\\n" +
            "                     |   /   _________________________   \\   |\n" +
            "                     |  |   |  __  __  _   _  ___ _____ |  |  |\n" +
            "                     |  |   | [  \\/  ]| | | |/ _ \\_   _]|  |  |\n" +
            "                     |  |   | [ |\\/| ]| |_| |  _  | | |  |  |  |\n" +
            "                     |  |   | [_|  |_]|_____/_| |_| |_|  |  |  |\n" +
            "                     |  |   |___________________________|  |  |\n" +
            "                     |  |             ( (O) )              |  |\n" +
            "                     |  |            /=======\\             |  |\n" +
            "                     |  |           | ===*=== |            |  |\n" +
            "                     |  |            \\=======/             |  |\n" +
            "                     |   \\                               /   |\n" +
            "                      \\   '-----------------------------'   /\n" +
            "                       '-----------------------------------'";

    public static final String SHIELD_SECURITY =
            "                                  .---.\n" +
            "                                 /_____\\\n" +
            "                                ( (@= ) )\n" +
            "                                 \\  =  /\n" +
            "                              .---'---'---.\n" +
            "                             /  SECURE-AUTH \\\n" +
            "                            |  [PBKDF2-HMAC] |\n" +
            "                            |   65k-HASHES   |\n" +
            "                             \\  LOCKOUT-ON  /\n" +
            "                              \\   3-TRIES  /\n" +
            "                               \\    ___   /\n" +
            "                                '-( V )-'\n" +
            "                                   '-'";

    public static final String SUCCESS_STAMP =
            "  +---------------------------------------------------+\n" +
            "  | [OK] TRANSACTION APPROVED & VERIFIED CRYPTOGRAPHICALLY |\n" +
            "  +---------------------------------------------------+";

    public static final String WARNING_STAMP =
            "  +---------------------------------------------------+\n" +
            "  | [!] SECURITY ALERT: TRANSACTION OR ACTION REJECTED |\n" +
            "  +---------------------------------------------------+";

    public static final String GOODBYE =
            "  ======================================================\n" +
            "     Thank you for banking with Secure Bank Systems.    \n" +
            "    Session terminated safely. Memory buffers wiped.    \n" +
            "  ======================================================";

    public static void printReceipt(String txId, String date, String type,
                                    String source, String dest, String amount, String balance) {
        System.out.println("  .--------------------------------------------------.");
        System.out.println("  |             OFFICIAL TRANSACTION RECEIPT         |");
        System.out.println("  |--------------------------------------------------|");
        System.out.printf("  | Ref ID       : %-33s |\n", txId);
        System.out.printf("  | Timestamp    : %-33s |\n", date);
        System.out.printf("  | Operation    : %-33s |\n", type);
        System.out.printf("  | From Account : %-33s |\n", source);
        System.out.printf("  | To / Channel : %-33s |\n", dest);
        System.out.printf("  | Amount       : $%-32s |\n", amount);
        System.out.printf("  | New Balance  : $%-32s |\n", balance);
        System.out.println("  | Status       : SUCCESS (Tamper-evident record)   |");
        System.out.println("  '--------------------------------------------------'");
    }

    /**
     * Renders a smooth animated progress bar on the terminal.
     *
     * @param task description of the operation
     * @param totalMs duration of the animation in milliseconds
     */
    public static void showProgress(String task, int totalMs) {
        int totalBlocks = 22;
        int interval = Math.max(10, totalMs / totalBlocks);
        System.out.print("  " + task + " ");
        for (int i = 0; i <= totalBlocks; i++) {
            StringBuilder bar = new StringBuilder("[");
            for (int j = 0; j < totalBlocks; j++) {
                if (j < i) bar.append("=");
                else if (j == i) bar.append(">");
                else bar.append(" ");
            }
            int pct = (i * 100) / totalBlocks;
            bar.append(String.format("] %3d%%", pct));
            System.out.print("\r  " + task + " " + bar.toString());
            System.out.flush();
            try {
                Thread.sleep(interval);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println(" [OK]");
    }

    /**
     * Displays a dynamic in-place rotating spinner for quick feedback.
     *
     * @param message operation description
     * @param iterations number of spinner cycles
     */
    public static void showSpinner(String message, int iterations) {
        char[] spin = new char[]{'|', '/', '-', '\\'};
        System.out.print("  " + message + "  ");
        for (int i = 0; i < iterations; i++) {
            System.out.print("\b" + spin[i % spin.length]);
            System.out.flush();
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("\b[READY]");
    }
}
