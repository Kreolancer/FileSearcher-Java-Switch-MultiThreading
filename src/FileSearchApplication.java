import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FileSearchApplication {

    private JFrame frame;
    private JMenuBar menuBar;
    private JMenu menu;
    private JMenuItem menuButton1;
    private JMenuItem menuButton2;
    private JMenuItem menuButton3;
    private JMenuItem menuButton4;
    private JMenuItem menuButton5;
    private JTextField directoryTextField;
    private JTextField directoryTextField1;
    private JTextField patternTextField;
    private JTextField patternTextField1;
    private JCheckBox recursiveCheckBox;
    private JCheckBox recursiveCheckBox1;
    private JCheckBox patternChechBox;
    private JCheckBox patternChechBox1;
    private JTextField maxDepthTextField;
    private JTextField maxDepthTextField1;
    private JButton searchButton;
    private JButton searchButton1;
    private JButton choosePathButton;
    private JButton choosePathButton1;
    private JButton stopButton;
    private JButton stopButton1;
    private JFileChooser fileChooser;
    private JFileChooser fileChooser1;
    private JTextArea resultTextArea;
    private Thread fileSearchThread = null;
    private Thread fileSearchThread1 = null;
    private final ReentrantLock lock = new ReentrantLock();
    private final ReentrantLock lock1 = new ReentrantLock();
    Condition suspend1 =  lock1.newCondition();
    AtomicBoolean needToEdit = new AtomicBoolean(true);
    Condition suspend =  lock.newCondition();
    AtomicBoolean running = new AtomicBoolean(false);
    AtomicBoolean paused = new AtomicBoolean(false);
    Process process = null;
    String fileSave = "";
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    FileSearchApplication window = new FileSearchApplication();
                    window.frame.setVisible(true);
                    JOptionPane.showMessageDialog(null, "Работа с обоими потоками одновременно происходит через меню в левом верхнем углу,\n" +
                            " это сделано из-за местами сильных лагов визуальных компонентов Swing\n" +
                            " и невозможности нажать несколько кнопок одновременно во время зависания\n");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public FileSearchApplication() {
        frame = new JFrame();
        frame.setBounds(0, 0, 1500, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Lab_3");

        menuBar = new JMenuBar();
        menu = new JMenu("Меню");
        menuButton1 = new JMenuItem("Старт");
        menuButton2 = new JMenuItem("Пауза");
        menuButton2.setEnabled(false);
        menuButton3 = new JMenuItem("Продолжить");
        menuButton3.setEnabled(false);
        menuButton4 = new JMenuItem("Стоп");
        menuButton4.setEnabled(false);
        menuButton5 = new JMenuItem("Очистить");

        menuButton1.addActionListener(e -> {
            needToEdit.set(true);
            running.set(true);
            menuButton2.setEnabled(true);
            menuButton4.setEnabled(true);
            menuButton1.setEnabled(false);
            stopButton.setEnabled(true);
            stopButton1.setEnabled(true);
            searchButton.doClick();
            searchButton1.doClick();
            searchButton.setEnabled(false);
            searchButton1.setEnabled(false);
        });

        menuButton2.addActionListener(e -> {
            paused.set(true);
            menuButton3.setEnabled(true);
            System.out.println("Пауза" + "\n");
            System.out.println("Thread " + fileSearchThread.getName() + " is " + fileSearchThread.getState() + "\t" + fileSearchThread.getPriority());
            System.out.println("Thread " + fileSearchThread1.getName() + " is " + fileSearchThread1.getState() + "\t" + fileSearchThread1.getPriority());
        });

        menuButton3.addActionListener(e -> {
            paused.set(false);
            menuButton3.setEnabled(false);

            lock.lock();
            try {
                suspend.signalAll();
            } catch (RuntimeException ex) {
                throw new RuntimeException(ex);
            }
            finally {
                lock.unlock();
            }

            System.out.println("Возобновить" + "\n");
            System.out.println("Thread " + fileSearchThread.getName() + " is " + fileSearchThread.getState() + "\t" + fileSearchThread.getPriority());
            System.out.println("Thread " + fileSearchThread1.getName() + " is " + fileSearchThread1.getState() + "\t" + fileSearchThread1.getPriority());
        });

        menuButton4.addActionListener(e -> {
            running.set(false);
            menuButton1.setEnabled(true);
            menuButton2.setEnabled(false);
            menuButton3.setEnabled(false);
            menuButton4.setEnabled(false);
            searchButton.setEnabled(true);
            searchButton1.setEnabled(true);
            System.out.println("Стоп" + "\n");

            lock.lock();
            try {
                suspend.signalAll();
                fileSearchThread.interrupt();
                fileSearchThread1.interrupt();
            } catch (RuntimeException ex) {
                throw new RuntimeException(ex);
            }
            finally {
                lock.unlock();
            }

            System.gc();
            System.out.println("Thread " + fileSearchThread.getName() + " is " + fileSearchThread.getState() + "\t" + fileSearchThread.getPriority());
            System.out.println("Thread " + fileSearchThread1.getName() + " is " + fileSearchThread1.getState() + "\t" + fileSearchThread1.getPriority());
        });

        menuButton5.addActionListener(e -> {
            resultTextArea.selectAll();
            resultTextArea.replaceSelection("");
            //needToEdit = true;
            System.out.println("Очистить" + "\n");
            System.out.println("Thread " + fileSearchThread.getName() + " is " + fileSearchThread.getState() + "\t" + fileSearchThread.getPriority());
            System.out.println("Thread " + fileSearchThread1.getName() + " is " + fileSearchThread1.getState() + "\t" + fileSearchThread1.getPriority());
        });

        menu.add(menuButton1);
        menu.add(menuButton2);
        menu.add(menuButton3);
        menu.add(menuButton4);
        menu.add(menuButton5);
        menuBar.add(menu);
        frame.setJMenuBar(menuBar);

        JPanel panel = new JPanel();
        panel.setSize(900, 100);
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 20 ,20));
        panel.setBackground(new Color(255,99,71));
        frame.getContentPane().add(panel, BorderLayout.NORTH);

        JPanel panel1 = new JPanel();
        panel1.setSize(900, 100);
        panel1.setLayout(new FlowLayout(FlowLayout.CENTER, 20 ,20));
        panel1.setBackground(new Color(218,165,32));
        frame.getContentPane().add(panel1, BorderLayout.SOUTH);

        JLabel directoryLabel = new JLabel("Директория");
        panel.add(directoryLabel);

        JLabel directoryLabel1 = new JLabel("Директория");
        panel1.add(directoryLabel1);

        directoryTextField = new JTextField();
        directoryTextField.setColumns(30);
        directoryTextField.setEditable(false);
        panel.add(directoryTextField);

        directoryTextField1 = new JTextField();
        directoryTextField1.setColumns(30);
        directoryTextField1.setEditable(false);
        panel1.add(directoryTextField1);

        choosePathButton = new JButton("Choose path");
        choosePathButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Выбор директории");
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = fileChooser.showOpenDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedDirectory = fileChooser.getSelectedFile();
                    directoryTextField.setText(selectedDirectory.getAbsolutePath());
                }
            }
        });
        panel.add(choosePathButton);

        choosePathButton1 = new JButton("Choose path");
        choosePathButton1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileChooser1 = new JFileChooser();
                fileChooser1.setDialogTitle("Выбор директории");
                fileChooser1.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result1 = fileChooser1.showOpenDialog(frame);
                if (result1 == JFileChooser.APPROVE_OPTION) {
                    File selectedDirectory1 = fileChooser1.getSelectedFile();
                    directoryTextField1.setText(selectedDirectory1.getAbsolutePath());
                }
            }
        });
        panel1.add(choosePathButton1);

        JLabel patternLabel = new JLabel("Pattern:");
        panel.add(patternLabel);

        JLabel patternLabel1 = new JLabel("Pattern:");
        panel1.add(patternLabel1);

        patternTextField = new JTextField();
        patternTextField.setColumns(20);
        panel.add(patternTextField);

        patternTextField1 = new JTextField();
        patternTextField1.setColumns(20);
        panel1.add(patternTextField1);

        recursiveCheckBox = new JCheckBox("Recursive");
        panel.add(recursiveCheckBox);

        patternChechBox = new JCheckBox("Шаблон");
        panel.add(patternChechBox);

        recursiveCheckBox1 = new JCheckBox("Recursive");
        panel1.add(recursiveCheckBox1);

        patternChechBox1 = new JCheckBox("Шаблон");
        panel1.add(patternChechBox1);

        JLabel maxDepthLabel = new JLabel("Max Depth:");
        panel.add(maxDepthLabel);

        JLabel maxDepthLabel1 = new JLabel("Max Depth:");
        panel1.add(maxDepthLabel1);

        maxDepthTextField = new JTextField("0");
        maxDepthTextField.setColumns(5);
        panel.add(maxDepthTextField);

        maxDepthTextField1 = new JTextField("0");
        maxDepthTextField1.setColumns(5);
        panel1.add(maxDepthTextField1);

        searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (patternTextField.getText().isEmpty() || directoryTextField.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Поля 'Directory' и 'Pattern' не могут быть пустыми");
                    menuButton1.setEnabled(true);
                    menuButton2.setEnabled(false);
                    menuButton3.setEnabled(false);
                    menuButton4.setEnabled(false);
                    searchButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    return;
                }
                if (maxDepthTextField.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Поля 'Max Depth' не могут быть пустыми");
                    menuButton1.setEnabled(true);
                    menuButton2.setEnabled(false);
                    menuButton3.setEnabled(false);
                    menuButton4.setEnabled(false);
                    searchButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    return;
                }
                if (recursiveCheckBox.isSelected() && (Integer.parseInt(maxDepthTextField.getText()) > 6 || Integer.parseInt(maxDepthTextField.getText()) < 0)) {
                    JOptionPane.showMessageDialog(frame, "Значение 'Max Depth' не должно превышать 6 и быть меньше нуля");
                    menuButton1.setEnabled(true);
                    menuButton2.setEnabled(false);
                    menuButton3.setEnabled(false);
                    menuButton4.setEnabled(false);
                    searchButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    return;
                }

                stopButton.setEnabled(true);
                searchButton.setEnabled(false);
                running.set(true);
                startSearchThreads();
            }
        });
        panel.add(searchButton);

        searchButton1 = new JButton("Search");
        searchButton1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (patternTextField1.getText().isEmpty() || directoryTextField1.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Поля 'Directory' и 'Pattern' не могут быть пустыми");
                    menuButton1.setEnabled(true);
                    menuButton2.setEnabled(false);
                    menuButton3.setEnabled(false);
                    menuButton4.setEnabled(false);
                    searchButton1.setEnabled(true);
                    stopButton1.setEnabled(false);
                    return;
                }
                if (maxDepthTextField1.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Поля 'Max Depth' не могут быть пустыми");
                    menuButton1.setEnabled(true);
                    menuButton2.setEnabled(false);
                    menuButton3.setEnabled(false);
                    menuButton4.setEnabled(false);
                    searchButton1.setEnabled(true);
                    stopButton1.setEnabled(false);
                    return;
                }
                if (recursiveCheckBox1.isSelected() && (Integer.parseInt(maxDepthTextField1.getText()) > 6 || Integer.parseInt(maxDepthTextField1.getText()) < 0)) {
                    JOptionPane.showMessageDialog(frame, "Значение 'Max Depth' не должно превышать 6 и быть меньше нуля");
                    menuButton1.setEnabled(true);
                    menuButton2.setEnabled(false);
                    menuButton3.setEnabled(false);
                    menuButton4.setEnabled(false);
                    searchButton1.setEnabled(true);
                    stopButton1.setEnabled(false);
                    return;
                }

                stopButton1.setEnabled(true);
                searchButton1.setEnabled(false);
                running.set(true);
                startSearchThreads1();
            }
        });
        panel1.add(searchButton1);

        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileSearchThread.interrupt();
                searchButton.setEnabled(true);
                stopButton.setEnabled(false);
                System.out.println("Thread " + fileSearchThread.getName() + " is running " + fileSearchThread.getState() + "\t" + fileSearchThread.getPriority());
            }
        });
        panel.add(stopButton);

        stopButton1 = new JButton("Stop");
        stopButton1.setEnabled(false);
        stopButton1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileSearchThread1.interrupt();
                searchButton1.setEnabled(true);
                stopButton1.setEnabled(false);
                System.out.println("Thread " + fileSearchThread1.getName() + " is running " + fileSearchThread1.getState() + "\t" + fileSearchThread1.getPriority());
            }
        });
        panel1.add(stopButton1);

        resultTextArea = new JTextArea();
        resultTextArea.setEditable(false);
        JScrollPane scrollPane1 = new JScrollPane(resultTextArea);
        scrollPane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane1.setPreferredSize(new Dimension(frame.getWidth() / 100 * 99 , frame.getHeight()));
        frame.getContentPane().add(scrollPane1, BorderLayout.WEST);
    }

    private void startSearchThreads() {
        if (fileSearchThread != null && !fileSearchThread.isInterrupted()) {
            fileSearchThread.interrupt();
            System.gc();
            searchButton.setEnabled(true);
        }

        String directoryPath = directoryTextField.getText();
        String pattern = patternTextField.getText();
        boolean recursive = recursiveCheckBox.isSelected();
        boolean patternSearch = patternChechBox.isSelected();
        int maxDepth = Integer.parseInt(maxDepthTextField.getText());

        searchButton.setEnabled(false);
        fileSearchThread = new Thread(new FileSearchRunnable(directoryPath, pattern, recursive, patternSearch, maxDepth));
        fileSearchThread.start();

        ThreadGroup currentGroup = Thread.currentThread().getThreadGroup();
        int noThreads = currentGroup.activeCount();
        Thread[] threads = new Thread[noThreads];
        currentGroup.enumerate(threads);
        for (Thread t : threads) {
            if (t != null) {
                System.out.println("Thread " + t.getName() + " is running " + t.getState() + "\t" + t.getPriority());
            }
        }
    }

    private void startSearchThreads1() {
        if (fileSearchThread1 != null && !fileSearchThread1.isInterrupted()) {
            fileSearchThread1.interrupt();
            System.gc();
            searchButton1.setEnabled(true);
        }

        String directoryPath1 = directoryTextField1.getText();
        String pattern1 = patternTextField1.getText();
        boolean recursive1 = recursiveCheckBox1.isSelected();
        boolean patternSearch1= patternChechBox1.isSelected();
        int maxDepth1 = Integer.parseInt(maxDepthTextField1.getText());

        searchButton1.setEnabled(false);
        fileSearchThread1 = new Thread(new FileSearchRunnable(directoryPath1, pattern1, recursive1, patternSearch1, maxDepth1));
        fileSearchThread1.start();

        ThreadGroup currentGroup = Thread.currentThread().getThreadGroup();
        int noThreads = currentGroup.activeCount();
        Thread[] threads = new Thread[noThreads];
        currentGroup.enumerate(threads);
        for (Thread t : threads) {
            if (t != null) {
                System.out.println("Thread " + t.getName() + " is " + t.getState() + "\t" + t.getPriority());
            }
        }
    }

    private class FileSearchRunnable implements Runnable {
        private String directoryPath;
        private String pattern;
        private boolean recursive;
        private boolean patternSearch;
        private int maxDepth;

        public FileSearchRunnable(String directoryPath, String pattern, boolean recursive, boolean patternSearch, int maxDepth) {
            this.directoryPath = directoryPath;
            this.pattern = pattern;
            this.recursive = recursive;
            this.patternSearch = patternSearch;
            this.maxDepth = maxDepth;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                if (running.get() && !paused.get()) {
                    searchFiles(new File(directoryPath), pattern, recursive, patternSearch, maxDepth);
                }
            }
        }

        private void searchFiles(File directory, String pattern, boolean recursive, boolean patternSearch, int maxDepth) {
            if (Thread.interrupted()) {
                return;
            }

            File[] files = directory.listFiles();
            if (files == null) {
                return;
            }

            for (File file : files) {
                if (file.isFile() && running.get() && !Thread.currentThread().isInterrupted()) {
                    if (paused.get()) {
                        lock.lock();
                        try {
                            suspend.await();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        finally {
                            lock.unlock();
                        }
                    }
                    if (!patternSearch) {
                        String fName = file.getName();
                        if (fName.contains(pattern)) {
                            synchronized (resultTextArea) {
                                SwingUtilities.invokeLater(() -> {
                                    resultTextArea.append(Thread.currentThread().getName() + "\t" + file.getAbsolutePath() + "\n");
                                });
                            }
                        }
                    } else {
                        String fName = file.getName();
                        String regex = "^" + pattern.replace(".", "\\.").replace("*", ".+") + "$";
                        if (fName.matches(regex)) {
                            synchronized (resultTextArea) {
                                SwingUtilities.invokeLater(() -> {
                                    resultTextArea.append(Thread.currentThread().getName() + "\t" + file.getAbsolutePath() + "\n");
                                });
                            }

                            if(file.getAbsolutePath().equals(fileSave)) {
                                lock1.lock();
                                try {
                                    process.waitFor();
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                                finally {
                                    lock1.unlock();
                                }
                            }
                            if (needToEdit.get() && fName.endsWith(".txt")) {
                                fileSave = file.getAbsolutePath();
                                needToEdit.set(false);
                                edit(file);
                            }
                        }
                        else {
                            JOptionPane.showMessageDialog(frame, "Введен паттерн, несоостветствующий шаблону. Введите корректный\n паттерн или отключите поиск по шаблону");
                            System.exit(0);
                        }
                    }
                } else if (recursive && file.isDirectory() && maxDepth != 0 && running.get() && !Thread.currentThread().isInterrupted()) {
                    if (paused.get()) {
                        lock.lock();
                        try {
                            suspend.await();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        finally {
                            lock.unlock();
                        }
                    }
                    searchFiles(file, pattern, recursive, patternSearch, maxDepth - 1);
                }
            }
        }

        private void edit(File file) {
            lock1.lock();
            try {
                ProcessBuilder pb = new ProcessBuilder("notepad.exe", file.getAbsolutePath());
                process = pb.start();
                process.waitFor();
                //suspend1.await();
            } catch (IOException | InterruptedException | RuntimeException e) {
                e.printStackTrace();
            }
            finally {
                lock1.unlock();
            }
        }
    }
}