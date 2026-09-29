package io.github.maybepritz.ui;

import io.github.maybepritz.model.PageInfo;
import io.github.maybepritz.service.ScanListener;
import io.github.maybepritz.service.WebScanner;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class App extends JFrame implements ScanListener {
    private final JTextField hostField = new JTextField("info.cern.ch");
    private final JTextField portField = new JTextField("80", 4);
    private final JSpinner maxPagesSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 500, 5));
    private final JButton startBtn = new JButton("Запустить обход");
    private final JLabel statusLabel = new JLabel("Готов к работе");

    private final JLabel totalPagesLabel = new JLabel("Всего страниц: 0");
    private final JLabel totalSizeLabel = new JLabel("Суммарный объем: 0 байт (0.00 КБ)");
    private final JLabel minPageLabel = new JLabel("<html><b>Мин. страница:</b> -</html>");
    private final JLabel maxPageLabel = new JLabel("<html><b>Макс. страница:</b> -</html>");

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final DefaultListModel<String> linksListModel = new DefaultListModel<>();
    private final JList<String> linksList = new JList<>(linksListModel);
    private final JTextArea logArea = new JTextArea(6, 40);

    private final List<PageInfo> loadedPages = new ArrayList<>();

    public App() {
        super("Парсер Приц 3 вариант");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Панель ввода
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Параметры HTTP-сервера"));
        inputPanel.add(new JLabel("Хост:"));
        hostField.setPreferredSize(new Dimension(200, 26));
        inputPanel.add(hostField);
        inputPanel.add(new JLabel("Порт:"));
        inputPanel.add(portField);
        inputPanel.add(new JLabel("Лимит страниц:"));
        inputPanel.add(maxPagesSpinner);
        startBtn.addActionListener(e -> onStartClicked());
        inputPanel.add(startBtn);

        mainPanel.add(inputPanel, BorderLayout.NORTH);

        // Панель метрик
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 8, 4));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Итого"));
        Font boldFont = totalPagesLabel.getFont().deriveFont(Font.BOLD, 12f);
        totalPagesLabel.setFont(boldFont);
        totalSizeLabel.setFont(boldFont);
        statsPanel.add(totalPagesLabel);
        statsPanel.add(totalSizeLabel);
        statsPanel.add(minPageLabel);
        statsPanel.add(maxPageLabel);

        // Таблица страниц
        String[] cols = {"#", "URL", "Код", "Размер (B)", "Ссылок"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(35);
        table.getColumnModel().getColumn(1).setPreferredWidth(450);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(85);
        table.getColumnModel().getColumn(4).setPreferredWidth(65);

        // При выборе строки в таблице показываем сохранённые в PageInfo ссылки
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = table.getSelectedRow();
                linksListModel.clear();
                if (selectedRow >= 0 && selectedRow < loadedPages.size()) {
                    PageInfo selectedPage = loadedPages.get(selectedRow);
                    for (String link : selectedPage.getLinks()) {
                        linksListModel.addElement(link);
                    }
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Обработанные страницы"));

        JScrollPane linksScroll = new JScrollPane(linksList);
        linksScroll.setPreferredSize(new Dimension(320, 0));
        linksScroll.setBorder(BorderFactory.createTitledBorder("Ссылки на выбранной странице"));

        // Сплит: Таблица слева, список ссылок справа
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, linksScroll);
        centerSplit.setResizeWeight(0.7);

        // Лог сокетов снизу
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Лог TCP-соединений"));

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, centerSplit, logScroll);
        mainSplit.setResizeWeight(0.75);

        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.add(statsPanel, BorderLayout.NORTH);
        contentPanel.add(mainSplit, BorderLayout.CENTER);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void onStartClicked() {
        String host = hostField.getText().trim().replace("http://", "").replace("https://", "");
        if (host.contains("/")) host = host.substring(0, host.indexOf('/'));

        if (host.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Введите адрес хоста!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Некорректный порт!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int maxPages = (int) maxPagesSpinner.getValue();

        // Сброс состояния
        loadedPages.clear();
        tableModel.setRowCount(0);
        linksListModel.clear();
        logArea.setText("");
        startBtn.setEnabled(false);
        statusLabel.setText("Обход запущен...");

        WebScanner scanner = new WebScanner(host, port, maxPages);
        new Thread(() -> scanner.scan(this)).start();
    }

    @Override
    public void onLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    @Override
    public void onPageFound(PageInfo page) {
        SwingUtilities.invokeLater(() -> {
            loadedPages.add(page);
            tableModel.addRow(new Object[]{
                    tableModel.getRowCount() + 1,
                    page.getUrl(),
                    page.getStatusCode(),
                    page.getSize(),
                    page.getLinksCount()
            });
        });
    }

    @Override
    public void onFinished(List<PageInfo> results) {
        SwingUtilities.invokeLater(() -> {
            startBtn.setEnabled(true);
            statusLabel.setText("Обход завершен. Всего страниц: " + results.size());

            if (results.isEmpty()) return;

            long total = 0;
            PageInfo min = results.get(0);
            PageInfo max = results.get(0);

            for (PageInfo p : results) {
                total += p.getSize();
                if (p.getSize() < min.getSize()) min = p;
                if (p.getSize() > max.getSize()) max = p;
            }

            totalPagesLabel.setText("Всего страниц: " + results.size());
            totalSizeLabel.setText(String.format("Суммарный объем: %,d байт (%.2f КБ)", total, total / 1024.0));
            minPageLabel.setText(String.format("<html><b>Мин. (%d B):</b> %s</html>", min.getSize(), min.getUrl()));
            maxPageLabel.setText(String.format("<html><b>Макс. (%d B):</b> %s</html>", max.getSize(), max.getUrl()));
        });
    }
}