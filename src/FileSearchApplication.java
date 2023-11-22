import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FileSearchApplication {

    private JFrame frame;
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
    private JButton clearButton;
    private JButton choosePathButton;
    private JButton choosePathButton1;
    private JFileChooser fileChooser;
    private JFileChooser fileChooser1;
    private JTextArea resultTextArea;
    private List<File> fileResultList = new ArrayList<>();
    private List<File> fileResultList1 = new ArrayList<>();
    private Thread fileSearchThread = null;
    private Thread fileSearchThread1 = null;
    private final ReentrantLock lock = new ReentrantLock();
    private boolean needToEdit = true;
    private final Object lock1 = new Object();
    private boolean isEditing = false;
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    FileSearchApplication window = new FileSearchApplication();
                    window.frame.setVisible(true);
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

        clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resultTextArea.selectAll();
                resultTextArea.replaceSelection("");
                needToEdit = true;
                System.out.println("Thread " + fileSearchThread.getName() + " is running " + fileSearchThread.getState() + "\t" + fileSearchThread.getPriority());
                System.out.println("Thread " + fileSearchThread1.getName() + " is running " + fileSearchThread1.getState() + "\t" + fileSearchThread1.getPriority());
            }
        });
        panel.add(clearButton);

        searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (patternTextField.getText().isEmpty() || directoryTextField.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Поля 'Directory' и 'Pattern' не могут быть пустыми");
                    return;
                }
                if (maxDepthTextField.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Поля 'Max Depth' не могут быть пустыми");
                    return;
                }
                if (recursiveCheckBox.isSelected() && (Integer.parseInt(maxDepthTextField.getText()) > 6 || Integer.parseInt(maxDepthTextField.getText()) < 0)) {
                    JOptionPane.showMessageDialog(frame, "Значение 'Max Depth' не должно превышать 6 и быть меньше нуля");
                    return;
                }

                startSearchThreads();
            }
        });
        panel.add(searchButton);

        searchButton1 = new JButton("Search");
        searchButton1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (patternTextField1.getText().isEmpty() || directoryTextField1.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Поля 'Directory' и 'Pattern' не могут быть пустыми");
                    return;
                }
                if (maxDepthTextField1.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Поля 'Max Depth' не могут быть пустыми");
                    return;
                }
                if (recursiveCheckBox1.isSelected() && (Integer.parseInt(maxDepthTextField1.getText()) > 6 || Integer.parseInt(maxDepthTextField1.getText()) < 0)) {
                    JOptionPane.showMessageDialog(frame, "Значение 'Max Depth' не должно превышать 6 и быть меньше нуля");
                    return;
                }

                startSearchThreads1();
            }
        });
        panel1.add(searchButton1);

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
            fileResultList.clear();
            searchButton.setEnabled(true);
        }

        if (fileSearchThread1 != null && !fileSearchThread1.isInterrupted()) {
            fileSearchThread1.interrupt();
            System.gc();
            fileResultList1.clear();
            searchButton1.setEnabled(true);
        }

        String directoryPath1 = directoryTextField1.getText();
        String pattern1 = patternTextField1.getText();
        boolean recursive1 = recursiveCheckBox1.isSelected();
        boolean patternSearch1= patternChechBox1.isSelected();
        int maxDepth1 = Integer.parseInt(maxDepthTextField1.getText());

        String directoryPath = directoryTextField.getText();
        String pattern = patternTextField.getText();
        boolean recursive = recursiveCheckBox.isSelected();
        boolean patternSearch = patternChechBox.isSelected();
        int maxDepth = Integer.parseInt(maxDepthTextField.getText());

        searchButton.setEnabled(false);
        fileSearchThread = new Thread(new FileSearchRunnable(directoryPath, pattern, recursive, patternSearch, maxDepth, fileResultList));
        fileSearchThread.start();

        fileSearchThread1 = new Thread(new FileSearchRunnable(directoryPath1, pattern1, recursive1, patternSearch1, maxDepth1, fileResultList1));
        fileSearchThread1.start();

        ThreadGroup currentGroup = Thread.currentThread().getThreadGroup();
        int noThreads = currentGroup.activeCount();
        Thread[] threads = new Thread[noThreads];
        currentGroup.enumerate(threads);
        for (Thread t : threads) {
            if (t != null) {
                System.out.println("Thread " + t.getName() + " is running " + t.getState() + "\t" + t.getPriority());
            }
        }

//        try {
//            fileSearchThread.join();
//            fileSearchThread1.join();
//        }
//        catch (InterruptedException ex) {
//            ex.printStackTrace();
//        }

        searchButton.setEnabled(true);
    }

    private void startSearchThreads1() {
        if (fileSearchThread1 != null && !fileSearchThread1.isInterrupted()) {
            fileSearchThread1.interrupt();
            System.gc();
            fileResultList1.clear();
            searchButton1.setEnabled(true);
        }

        String directoryPath1 = directoryTextField1.getText();
        String pattern1 = patternTextField1.getText();
        boolean recursive1 = recursiveCheckBox1.isSelected();
        boolean patternSearch1= patternChechBox1.isSelected();
        int maxDepth1 = Integer.parseInt(maxDepthTextField1.getText());

        searchButton1.setEnabled(false);
        fileSearchThread1 = new Thread(new FileSearchRunnable(directoryPath1, pattern1, recursive1, patternSearch1, maxDepth1, fileResultList1));
        fileSearchThread1.start();

//        try {
//            fileSearchThread1.join();
//        } catch (InterruptedException ex) {
//            ex.printStackTrace();
//        }

        ThreadGroup currentGroup = Thread.currentThread().getThreadGroup();
        int noThreads = currentGroup.activeCount();
        Thread[] threads = new Thread[noThreads];
        currentGroup.enumerate(threads);
        for (Thread t : threads) {
            if (t != null) {
                System.out.println("Thread " + t.getName() + " is " + t.getState() + "\t" + t.getPriority());
            }
        }

        searchButton1.setEnabled(true);
    }

    private class FileSearchRunnable implements Runnable {
        private String directoryPath;
        private String pattern;
        private boolean recursive;
        private boolean patternSearch;
        private int maxDepth;
        private List<File> fileResultList2;

        public FileSearchRunnable(String directoryPath, String pattern, boolean recursive, boolean patternSearch, int maxDepth, List<File> fileResultList2) {
            this.directoryPath = directoryPath;
            this.pattern = pattern;
            this.recursive = recursive;
            this.patternSearch = patternSearch;
            this.maxDepth = maxDepth;
            this.fileResultList2 = fileResultList2;
        }

        @Override
        public void run() {
            searchFiles(new File(directoryPath), pattern, recursive, patternSearch, maxDepth, fileResultList2);
        }

        private void searchFiles(File directory, String pattern, boolean recursive, boolean patternSearch, int maxDepth, List<File> fileResultList2) {
            if (Thread.interrupted()) {
                return;
            }

            File[] files = directory.listFiles();
            if (files == null) {
                return;
            }

            for (File file : files) {
                if (file.isFile()) {
                    if (!patternSearch) {
                        String fName = file.getName();
                        if (fName.contains(pattern)) {
                            //fileResultList2.add(file);
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

                            //fileResultList2.add(file);
                            synchronized (resultTextArea) {
                                SwingUtilities.invokeLater(() -> {
                                    resultTextArea.append(Thread.currentThread().getName() + "\t" + file.getAbsolutePath() + "\n");
                                });
                            }

                            if(needToEdit && fName.endsWith(".txt")) {
                                //String text = file.getAbsoluteFile();
                                needToEdit = false;
                                editFile(file);
                            }
                        }
                    }
                } else if (recursive && file.isDirectory() && maxDepth != 0) {
                    searchFiles(file, pattern, recursive, patternSearch, maxDepth - 1, fileResultList2);
                }
            }
            //StringBuilder sb = new StringBuilder();
            //for (File file : fileResultList2) {
            //    sb.append(file.getAbsolutePath()).append("\n");
            //}
            //resultTextArea2.append(sb.toString());
        }

        private void edit(File file) {
            //lock.lock();
            try {

                if (!Desktop.isDesktopSupported()) {
                    System.out.println("Desktop is not supported");
                    return;
                }

                Desktop desktop = Desktop.getDesktop(); //тут приложение по умолчанию винды используется
                if (file.exists()) {
                    desktop.edit(file);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                //lock.unlock();
            }
        }
//Condition suspend =  lock.newCondition();
        //private void editFile(File file) {
            //lock.lock();
            //try {
            //    while (isEditing) {
            //
            //            suspend.await();  // Поток ожидает, пока не получит уведомление
            //
            //    }
        //} catch (InterruptedException e) {
        //    e.printStackTrace();
        //}finally {
        //    lock.unlock();
        //    }
        //        isEditing = true;  // Поток начинает редактирование файла

                // Запуск файла на редактирование
                // Предположим, что 'edit' - это метод, который открывает файл для редактирования
                //edit(file);

                //isEditing = false;  // Поток завершил редактирование файла
                //lock1.notifyAll();  // Уведомление всех ожидающих потоков
            //}

        private void editFile(File file) {
            synchronized(lock1) {
                while (isEditing) {
                    try {
                        lock1.wait();  // Поток ожидает, пока не получит уведомление
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                isEditing = true;  // Поток начинает редактирование файла

                // Запуск файла на редактирование
                // Предположим, что 'edit' - это метод, который открывает файл для редактирования
                edit(file);

                isEditing = false;  // Поток завершил редактирование файла
                lock1.notifyAll();  // Уведомление всех ожидающих потоков
            }
        }
    }
}