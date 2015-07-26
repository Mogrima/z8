package org.zenframework.z8.web.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.universalchardet.UniversalDetector;
import org.zenframework.z8.server.base.file.FileInfo;
import org.zenframework.z8.server.base.table.system.Properties;
import org.zenframework.z8.server.engine.ISession;
import org.zenframework.z8.server.engine.ServerInfo;
import org.zenframework.z8.server.json.Json;
import org.zenframework.z8.server.logs.Trace;
import org.zenframework.z8.server.types.encoding;
import org.zenframework.z8.server.types.guid;
import org.zenframework.z8.server.types.string;
import org.zenframework.z8.server.utils.IOUtils;
import org.zenframework.z8.web.servlet.Servlet;

import com.artofsolving.jodconverter.DocumentConverter;
import com.artofsolving.jodconverter.openoffice.connection.OpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.OpenOfficeDocumentConverter;

/**
 * Created with IntelliJ IDEA. User: volkov Date: 29.10.14 Time: 13:57 To change
 * this template use File | Settings | File Templates.
 */
public class ConverterAdapter extends Adapter implements Properties.Listener {

    private static final Log LOG = LogFactory.getLog(ConverterAdapter.class);

    private static final String CACHE_FOLDER_NAME = "pdf.cache";

    private static final String OFFICE_EXEC_EXT = SystemUtils.OS_NAME.toLowerCase().startsWith("windows") ? ".exe" : ".bin";
    private static final String OFFICE_EXEC_REL_PATH = "program/soffice" + OFFICE_EXEC_EXT;

    private int OFFICE_PORT = 8100;

    private volatile File officeHome;
    private volatile OpenOfficeConnection officeConnection;
    private volatile DocumentConverter pdfConverter;
    private Process officeProcess;

    private final static List<String> convertableExtensions = Arrays.asList("doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "odt", "odp", "ods", "odf", "odg", "wpd", "sxw", "sxi", "sxc", "sxd", "stw", "tif", "tiff", "vsd");

    public ConverterAdapter(Servlet servlet) {
        super(servlet);
    }

    @Override
    public void onPropertyChange(String key, String value) {
        if (ServletRuntime.LibreOfficeDirectoryProperty.equalsKey(key)) {
            officeHome = new File(value);
            stopOfficeManager();
        }
    }

    private void startOfficeManager() {
        if (officeConnection == null) {
            officeConnection = new SocketOpenOfficeConnection(OFFICE_PORT);
            try {
                officeConnection.connect();
            } catch (ConnectException e) {
                File sofficeBin = new File(getOfficeHome(), OFFICE_EXEC_REL_PATH);
                try {
                    if (sofficeBin.exists()) {
                        officeProcess = Runtime.getRuntime().exec(
                                "\"" + getOfficeHome()
                                        + "/program/soffice\" -headless -accept=\"socket,host=127.0.0.1,port=" + OFFICE_PORT
                                        + ";urp;\" -nofirststartwizard");
                    } else {
                        LOG.warn("Incorrect Office home. File " + sofficeBin + " doesn't exist. Can't start soffice process");
                    }
                    officeConnection.connect();
                } catch (IOException e1) {
                    throw new RuntimeException("Can't open OpenOffice connection", e);
                }
            }
            pdfConverter = new OpenOfficeDocumentConverter(officeConnection);
        }
    }

    private void stopOfficeManager() {
        if (officeConnection != null) {
            officeConnection.disconnect();
            officeConnection = null;
            pdfConverter = null;
        }
        if (officeProcess != null) {
            officeProcess.destroy();
        }
    }

    @Override
    public boolean canHandleRequest(HttpServletRequest request) {
        return "get".equalsIgnoreCase(request.getMethod());
    }

    @Override
    public void stop() {
        stopOfficeManager();
    }

    @Override
    protected void service(ISession session, Map<String, String> parameters, List<FileInfo> files,
            HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Корректная обработка ссылки относительно корня приложения
        String requestURI = URLDecoder.decode(request.getRequestURI(), encoding.Default.toString());
        String contextPath = request.getContextPath() + '/';

        if (requestURI.startsWith(contextPath)) {
            requestURI = requestURI.substring(contextPath.length());
        }

        File relativePath = new File(requestURI);
        File absolutePath = new File(super.getServlet().getServletPath(), requestURI);

        boolean preview = request.getParameter("preview") != null;

        if (!absolutePath.exists()) {

            FileInfo fileInfo = new FileInfo();
            fileInfo.path = new string(relativePath.toString());
            fileInfo.name = new string(absolutePath.getName());
            fileInfo.id = new guid(parameters.get(Json.recordId));
            downloadFile(session.getServerInfo(), fileInfo, absolutePath);
        }

        if (preview && isConvertableToPDFFileExtension(absolutePath)) {
            absolutePath = getConvertedPDF(relativePath, absolutePath);
        }

        if (preview) {
            if (isConvertableToPDFFileExtension(absolutePath)) {
                absolutePath = getConvertedPDF(relativePath, absolutePath);
                response.addHeader("Content-Type", "application/pdf");
            } else {
                String contentType = determContentType(absolutePath);
                if (contentType != null) {
                    if (contentType.startsWith("text/")) {
                        String encoding = determineEncoding(absolutePath);
                        contentType += "; charset=" + encoding;
                    } else if (contentType.equals("message/rfc822")) {
                        absolutePath = getConvertedEML(relativePath, absolutePath);
                        contentType = "text/plain; charset=UTF-8";
                    }
                }
                response.addHeader("Content-Type", contentType);
            }
        } else {
            response.addHeader("Content-Type", "application/*");
            response.addHeader("Content-Disposition", getContentDisposition(request, absolutePath.getName()));
        }
        sendFileToResponse(response, absolutePath);
    }

    private void downloadFile(ServerInfo serverInfo, FileInfo fileInfo, File path) throws IOException {
        String fileInfoPath = fileInfo != null ? fileInfo.path.get() : null;
        fileInfo = serverInfo.getAppServer().download(fileInfo);
        if (fileInfo != null) {
            FileUtils.copyInputStreamToFile(fileInfo.getInputStream(), path);
        } else {
            throw new IOException("File '" + fileInfoPath + "' does not exist");
        }
    }

    public File getConvertedPDF(File relativePath, File srcFile) throws IOException {
        File convertedFile = new File(new File(super.getServlet().getServletPath(), CACHE_FOLDER_NAME), relativePath
                + ".pdf");
        if (!convertedFile.exists()) {
            convertFileToPDF(srcFile, convertedFile);
        }
        return convertedFile;
    }

    public File getConvertedEML(File relativePath, File srcFile) throws IOException {
        File convertedFile = new File(new File(super.getServlet().getServletPath(), CACHE_FOLDER_NAME), relativePath
                + ".txt");
        if (!convertedFile.exists()) {
            convertEmlToTxt(srcFile, convertedFile);
        }
        return convertedFile;
    }

    private File getOfficeHome() {
        if (officeHome == null)
            officeHome = new File(Properties.getProperty(ServletRuntime.LibreOfficeDirectoryProperty));
        return officeHome;
    }

    private void initJODConverter() {
        if (officeConnection != null)
            return;

        synchronized (this) {
            startOfficeManager();
        }
    }

    private void sendFileToResponse(HttpServletResponse response, File file) throws IOException {
        FileInputStream input = new FileInputStream(file);
        OutputStream output = response.getOutputStream();
        try {
            IOUtils.copy(input, output);
        } finally {
            output.close();
            input.close();
        }
    }

    private void convertFileToPDF(File sourceFile, File convertedPDF) {
        initJODConverter();
        pdfConverter.convert(sourceFile, convertedPDF);
    }

    private static void convertEmlToTxt(File sourceFile, File convertedTxt) {
        convertedTxt.getParentFile().mkdirs();
        java.util.Properties props = new java.util.Properties();
        props.put("mail.host", "smtp.dummydomain.com");
        props.put("mail.transport.protocol", "smtp");
        Session mailSession = Session.getDefaultInstance(props, null);
        InputStream in = null;
        PrintStream out = null;
        try {
            in = new FileInputStream(sourceFile);
            out = new PrintStream(convertedTxt, "UTF-8");
            MimeMessage message = new MimeMessage(mailSession, in);
            out.println("Тема : " + message.getSubject());
            out.println("Отправитель : " + message.getFrom()[0]);
            out.println("----------------------------");
            out.println("Сообщение :");
            out.print(EmlUtil.parsePartDocText(message));
        } catch (Exception e) {
            Trace.logError("Can't convert EML to TXT", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {}
            }
            if (out != null) {
                out.close();
            }
        }
    }

    private boolean isConvertableToPDFFileExtension(File srcFile) {
        String fileName = srcFile.getName();
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();
        return convertableExtensions.contains(extension);
    }

    private String determContentType(File file) {
        String contentType = getServlet().getServletContext().getMimeType(file.getName().toLowerCase());
        return contentType != null ? contentType : "text/plain";
    }

    private String getContentDisposition(HttpServletRequest request, String fileName) throws UnsupportedEncodingException {
        String agent = request.getHeader("USER-AGENT").toLowerCase();

        if (agent == null) {
            return "attachment;filename=\"" + MimeUtility.encodeText(fileName, "utf8", "B") + "\"";
        }

        if (agent.contains("msie")) {
            return "attachment;filename=\"" + toHexString(fileName) + "\"";
        }

        if (agent.contains("opera")) {
            int version = -1;

            try {
                int prefixIndex = agent.indexOf("opera ");

                if (prefixIndex == -1) {
                    prefixIndex = agent.indexOf("opera/");
                }

                int startIndex = prefixIndex + "opera/".length();
                int stopIndex = agent.indexOf(".", startIndex);

                if (stopIndex == -1) {
                    stopIndex = agent.indexOf(" ", startIndex);
                }

                version = new Integer(agent.substring(startIndex, stopIndex)).intValue();
            } catch (Exception ex) {}

            if (version < 9 && version > -1) {
                // Opera 8.x and before
                return "attachment;filename=\"" + fileName + "\"";
            } else {
                // Opera 9 or later (or unkown) (encoding according to RFC2231)
                return "attachment;filename*=utf8''" + toHexString(fileName);
            }
        }

        return "attachment;filename=\"" + MimeUtility.encodeText(fileName, "utf8", "B") + "\"";
    }

    private String toHexString(String s) throws UnsupportedEncodingException {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (0 <= c && c <= 255 && !Character.isWhitespace(c)) {
                sb.append(c);
            } else {
                byte[] bytes = Character.toString(c).getBytes("utf8");
                for (int j = 0; j < bytes.length; j++) {
                    int k = bytes[j];
                    if (k < 0)
                        k += 256;
                    sb.append("%" + Integer.toHexString(k).toUpperCase());
                }
            }
        }
        return sb.toString();
    }

    private String determineEncoding(File file) {
        UniversalDetector detector = new UniversalDetector(null);
        // Reset detector before using
        detector.reset();
        // Buffer
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] buf = new byte[1024];
            for (int n = in.read(buf); n >= 0 && !detector.isDone(); n = in.read(buf)) {
                detector.handleData(buf, 0, n);
            }
            detector.dataEnd();
            return detector.getDetectedCharset();
        } catch (Exception e) {
            Trace.logError("Can't detect encoding of '" + file.getAbsolutePath() + "'", e);
            return "UTF-8";
        } finally {
            detector.reset();
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {}
            }
        }
    }

}