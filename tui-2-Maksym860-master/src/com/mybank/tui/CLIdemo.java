package com.mybank.tui;

import com.mybank.domain.Bank;
import com.mybank.domain.CheckingAccount;
import com.mybank.domain.Customer;
import com.mybank.domain.SavingsAccount;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import org.jline.reader.*;
import org.jline.reader.impl.completer.*;
import org.jline.utils.*;
import org.fusesource.jansi.*;

/**
 * Консольний клієнт для прикладу 'Banking'
 *
 * @author Maksym
 */
public class CLIdemo {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    private String[] commandsList;

    public void init() {
        commandsList = new String[]{"help", "customers", "customer", "report", "exit"};
    }

    /**
     * Завантажує клієнтів з файлу test.dat (завдання на "4")
     * Формат файлу:
     *   <кількість_клієнтів>
     *   (порожній рядок)
     *   <Ім'я> <Прізвище> <кількість_рахунків>
     *   S <баланс> <відсоткова_ставка>  (ощадний рахунок)
     *   C <баланс> <овердрафт>          (розрахунковий рахунок)
     *   ...
     */
    private void loadCustomers(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            int numCustomers = Integer.parseInt(br.readLine().trim());

            for (int i = 0; i < numCustomers; i++) {
                // skip blank lines
                String line = br.readLine();
                while (line != null && line.trim().isEmpty()) {
                    line = br.readLine();
                }
                if (line == null) break;

                String[] parts = line.trim().split("\\s+");
                String firstName = parts[0];
                String lastName = parts[1];
                int numAccounts = Integer.parseInt(parts[2]);

                Bank.addCustomer(firstName, lastName);
                Customer cust = Bank.getCustomer(Bank.getNumberOfCustomers() - 1);

                for (int j = 0; j < numAccounts; j++) {
                    line = br.readLine();
                    if (line == null) break;
                    String[] accParts = line.trim().split("\\s+");
                    String accType = accParts[0];
                    double balance = Double.parseDouble(accParts[1]);

                    if ("S".equals(accType)) {
                        double interestRate = Double.parseDouble(accParts[2]);
                        cust.addAccount(new SavingsAccount(balance, interestRate));
                    } else if ("C".equals(accType)) {
                        double overdraftAmount = Double.parseDouble(accParts[2]);
                        cust.addAccount(new CheckingAccount(balance, overdraftAmount));
                    }
                }
            }
            System.out.println(ANSI_GREEN + "Data loaded from file: " + filePath + ANSI_RESET);
        } catch (IOException e) {
            System.out.println(ANSI_RED + "ERROR! Could not load data from file: " + filePath + ANSI_RESET);
            System.out.println(ANSI_YELLOW + "Using default test data instead." + ANSI_RESET);
            loadDefaultCustomers();
        } catch (Exception e) {
            System.out.println(ANSI_RED + "ERROR! Invalid data format in file: " + e.getMessage() + ANSI_RESET);
            System.out.println(ANSI_YELLOW + "Using default test data instead." + ANSI_RESET);
            loadDefaultCustomers();
        }
    }

    private void loadDefaultCustomers() {
        Bank.addCustomer("John", "Doe");
        Bank.addCustomer("Fox", "Mulder");
        Bank.getCustomer(0).addAccount(new CheckingAccount(2000));
        Bank.getCustomer(1).addAccount(new SavingsAccount(1000, 3));
    }

    public void run() {
        AnsiConsole.systemInstall(); // потрібно для підтримки ANSI у Windows cmd
        printWelcomeMessage();
        LineReaderBuilder readerBuilder = LineReaderBuilder.builder();
        List<Completer> completors = new LinkedList<Completer>();

        completors.add(new StringsCompleter(commandsList));
        readerBuilder.completer(new ArgumentCompleter(completors));

        LineReader reader = readerBuilder.build();

        String line;
        PrintWriter out = new PrintWriter(System.out);

        while ((line = readLine(reader, "")) != null) {
            if ("help".equals(line)) {
                printHelp();
            } else if ("customers".equals(line)) {
                printCustomersList();
            } else if (line.indexOf("customer") != -1) {
                printCustomerDetails(line);
            } else if ("report".equals(line)) {
                printReport();
            } else if ("exit".equals(line)) {
                System.out.println("Exiting application");
                return;
            } else {
                System.out
                        .println(ANSI_RED + "Invalid command, For assistance press TAB or type \"help\" then hit ENTER." + ANSI_RESET);
            }
        }

        AnsiConsole.systemUninstall();
    }

    private void printCustomersList() {
        AttributedStringBuilder a = new AttributedStringBuilder()
                .append("\nThis is all of your ")
                .append("customers", AttributedStyle.BOLD.foreground(AttributedStyle.RED))
                .append(":");

        System.out.println(a.toAnsi());
        if (Bank.getNumberOfCustomers() > 0) {
            System.out.println("\nLast name\tFirst Name\tBalance");
            System.out.println("---------------------------------------");
            for (int i = 0; i < Bank.getNumberOfCustomers(); i++) {
                System.out.println(Bank.getCustomer(i).getLastName() + "\t\t"
                        + Bank.getCustomer(i).getFirstName() + "\t\t$"
                        + Bank.getCustomer(i).getAccount(0).getBalance());
            }
        } else {
            System.out.println(ANSI_RED + "Your bank has no customers!" + ANSI_RESET);
        }
    }

    private void printCustomerDetails(String line) {
        try {
            int custNo = 0;
            if (line.length() > 8) {
                String strNum = line.split(" ")[1];
                if (strNum != null) {
                    custNo = Integer.parseInt(strNum);
                }
            }
            Customer cust = Bank.getCustomer(custNo);
            String accType = cust.getAccount(0) instanceof CheckingAccount ? "Checking" : "Savings";

            AttributedStringBuilder a = new AttributedStringBuilder()
                    .append("\nThis is detailed information about customer #")
                    .append(Integer.toString(custNo), AttributedStyle.BOLD.foreground(AttributedStyle.RED))
                    .append("!");

            System.out.println(a.toAnsi());

            System.out.println("\nLast name\tFirst Name\tAccount Type\tBalance");
            System.out.println("-------------------------------------------------------");
            System.out.println(cust.getLastName() + "\t\t" + cust.getFirstName()
                    + "\t\t" + accType + "\t$" + cust.getAccount(0).getBalance());
        } catch (Exception e) {
            System.out.println(ANSI_RED + "ERROR! Wrong customer number!" + ANSI_RESET);
        }
    }

    /**
     * Виводить звіт по всіх клієнтах (завдання на "5")
     * Аналогічно до CustomerReport з лаби 8
     */
    private void printReport() {
        AttributedStringBuilder a = new AttributedStringBuilder()
                .append("\n")
                .append("CUSTOMER REPORT", AttributedStyle.BOLD.foreground(AttributedStyle.CYAN))
                .append("\n");
        System.out.println(a.toAnsi());

        if (Bank.getNumberOfCustomers() == 0) {
            System.out.println(ANSI_RED + "Your bank has no customers!" + ANSI_RESET);
            return;
        }

        System.out.println("====================================================");
        for (int i = 0; i < Bank.getNumberOfCustomers(); i++) {
            Customer cust = Bank.getCustomer(i);
            System.out.println("\nCustomer #" + i + ": "
                    + ANSI_GREEN + cust.getLastName() + ", " + cust.getFirstName() + ANSI_RESET);
            System.out.println("  Accounts:");
            System.out.println("  " + String.format("%-5s %-15s %s", "No.", "Type", "Balance"));
            System.out.println("  " + "-".repeat(35));

            double totalBalance = 0;
            for (int j = 0; j < cust.getNumberOfAccounts(); j++) {
                String accType;
                double balance = cust.getAccount(j).getBalance();
                totalBalance += balance;

                if (cust.getAccount(j) instanceof CheckingAccount) {
                    accType = "Checking";
                } else if (cust.getAccount(j) instanceof SavingsAccount) {
                    accType = "Savings";
                } else {
                    accType = "Unknown";
                }

                System.out.println("  " + String.format("%-5d %-15s $%.2f", j, accType, balance));
            }
            System.out.println("  " + "-".repeat(35));
            System.out.println("  Total balance: " + ANSI_YELLOW + "$" + String.format("%.2f", totalBalance) + ANSI_RESET);
        }
        System.out.println("\n====================================================");
        System.out.println("Total customers: " + ANSI_CYAN + Bank.getNumberOfCustomers() + ANSI_RESET);
    }

    private void printWelcomeMessage() {
        System.out
                .println("\nWelcome to " + ANSI_GREEN + " MyBank Console Client App" + ANSI_RESET
                        + "! \nFor assistance press TAB or type \"help\" then hit ENTER.");
    }

    private void printHelp() {
        System.out.println("help\t\t\t- Show help");
        System.out.println("customers\t\t- Show list of customers");
        System.out.println("customer 'index'\t- Show customer details");
        System.out.println("report\t\t\t- Show full customer report");
        System.out.println("exit\t\t\t- Exit the app");
    }

    private String readLine(LineReader reader, String promtMessage) {
        try {
            String line = reader.readLine(promtMessage + ANSI_YELLOW + "\nbank> " + ANSI_RESET);
            return line.trim();
        } catch (UserInterruptException e) {
            // наприклад ^C
            return null;
        } catch (EndOfFileException e) {
            // наприклад ^D
            return null;
        }
    }

    public static void main(String[] args) {
        CLIdemo shell = new CLIdemo();
        shell.init();
        // завдання на "4": завантажити дані з файлу
        shell.loadCustomers("data/test.dat");
        shell.run();
    }
}
