package zip;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class CenOpGUI {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(CenOpGUI::createAndShowGUI);
    }

    public static void operateAndRelease(String path, String method) throws Exception {
        CenOp.operate(path, method);
        System.gc(); // 建议性释放
        Thread.sleep(200); // 给 JVM 一点时间释放资源
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("ZipCenOpsGUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 300);
        frame.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField inputField = new JTextField();
        JTextField outputField = new JTextField();
        JButton inputBrowse = new JButton("选择文件");
        JButton outputBrowse = new JButton("选择文件");
        JButton recoverButton = new JButton("还原(PKZip)");
        JButton encryptButton = new JButton("加密(假加密)");
        JTextArea resultArea = new JTextArea("请选择文件或将文件拖拽到窗口内\n");
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);

        final File[] inputFile = new File[1];
        final File[] outputFile = new File[1];

        // 布局 Row 1: 输入框
        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(new JLabel("待处理文件:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        topPanel.add(inputField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        topPanel.add(inputBrowse, gbc);

        gbc.gridx = 3;
        topPanel.add(recoverButton, gbc);

        // 布局 Row 2: 输出框
        gbc.gridx = 0;
        gbc.gridy = 1;
        topPanel.add(new JLabel("输出路径:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        topPanel.add(outputField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        topPanel.add(outputBrowse, gbc);

        gbc.gridx = 3;
        topPanel.add(encryptButton, gbc);

        // 文件选择按钮
        inputBrowse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("ZIP Files", "zip"));
            int ret = chooser.showOpenDialog(frame);
            if (ret == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                if (!isZipFile(selected)) {
                    JOptionPane.showMessageDialog(frame, "仅支持 ZIP 文件", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                inputFile[0] = selected;
                inputField.setText(inputFile[0].getAbsolutePath());

                File defaultOutput = getDefaultOutputFile(inputFile[0]);
                outputFile[0] = defaultOutput;
                outputField.setText(defaultOutput.getAbsolutePath());
            }
        });

        outputBrowse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("output.zip"));
            int ret = chooser.showSaveDialog(frame);
            if (ret == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                String path = selected.getAbsolutePath();
                if (!path.toLowerCase().endsWith(".zip")) {
                    selected = new File(path + ".zip");
                }
                outputFile[0] = selected;
                outputField.setText(selected.getAbsolutePath());
            }
        });

        recoverButton.addActionListener(e -> handleOperation("r", inputFile[0], outputFile[0], resultArea));
        encryptButton.addActionListener(e -> handleOperation("e", inputFile[0], outputFile[0], resultArea));

        // 添加拖拽支持
        addZipDropSupport(frame.getContentPane(), inputFile, outputFile, inputField, outputField, frame);
        addZipDropSupport(inputField, inputFile, outputFile, inputField, outputField, frame);
        addZipDropSupport(outputField, inputFile, outputFile, inputField, outputField, frame);
        addZipDropSupport(resultArea, inputFile, outputFile, inputField, outputField, frame);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void handleOperation(String method, File input, File output, JTextArea resultArea) {
        if (input == null || output == null) {
            JOptionPane.showMessageDialog(null, "请先选择待处理文件和输出路径。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Files.copy(input.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
            operateAndRelease(output.getAbsolutePath(), method);
            resultArea.append("✔ 操作成功：" + output.getAbsolutePath() + "\n");

            Desktop.getDesktop().open(output.getParentFile());
        } catch (Exception ex) {
            resultArea.append("✘ 操作失败：" + ex.getMessage() + "\n");
            ex.printStackTrace();
        }
    }

    // 判断是否为 ZIP 文件
    private static boolean isZipFile(File file) {
        return file.isFile() && file.getName().toLowerCase().endsWith(".zip");
    }

    // 获取默认输出文件路径
    private static File getDefaultOutputFile(File inputFile) {
        String parent = inputFile.getParent();
        String name = inputFile.getName();
        int dot = name.lastIndexOf('.');
        String baseName = dot > 0 ? name.substring(0, dot) : name;
        return new File(parent, baseName + "_mod.zip");
    }

    // 通用拖拽注册器
    @SuppressWarnings("unchecked")
    private static void addZipDropSupport(Component comp, File[] inputFile, File[] outputFile,
                                          JTextField inputField, JTextField outputField, JFrame frame) {
        new DropTarget(comp, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    java.util.List<File> droppedFiles =
                            (java.util.List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!droppedFiles.isEmpty()) {
                        File dropped = droppedFiles.get(0);
                        if (!isZipFile(dropped)) {
                            JOptionPane.showMessageDialog(frame, "仅支持 ZIP 文件", "错误", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        inputFile[0] = dropped;
                        inputField.setText(inputFile[0].getAbsolutePath());

                        File defaultOutput = getDefaultOutputFile(inputFile[0]);
                        outputFile[0] = defaultOutput;
                        outputField.setText(defaultOutput.getAbsolutePath());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
