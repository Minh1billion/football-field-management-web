package utescore.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import utescore.config.VnPayConfig;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VnPayService {

    private final VnPayConfig vnPayConfig;

    @Autowired
    public VnPayService(VnPayConfig vnPayConfig) {
        this.vnPayConfig = vnPayConfig;
    }

    public String createOrder(int total, String orderInfo, String urlReturn) {
        String vnpVersion = "2.1.0";
        String vnpCommand = "pay";
        String vnpTxnRef = VnPayConfig.getRandomNumber(8);
        String vnpIpAddr = "127.0.0.1";
        String orderType = "other";

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnpVersion);
        vnpParams.put("vnp_Command", vnpCommand);
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(total * 100));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", vnpTxnRef);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", orderType);
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", urlReturn + "/user/rentals/payment/callback");
        vnpParams.put("vnp_IpAddr", vnpIpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnpParams.put("vnp_CreateDate", formatter.format(cld.getTime()));
        cld.add(Calendar.MINUTE, 15);
        vnpParams.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (int i = 0; i < fieldNames.size(); i++) {
            String name = fieldNames.get(i);
            String value = vnpParams.get(name);
            if (value != null && !value.isEmpty()) {
                try {
                    String encodedValue = URLEncoder.encode(value, "UTF-8");
                    hashData.append(name).append("=").append(encodedValue);
                    query.append(URLEncoder.encode(name, "UTF-8"))
                            .append("=")
                            .append(encodedValue);
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("UTF-8 encoding not supported", e);
                }
                if (i < fieldNames.size() - 1) {
                    hashData.append("&");
                    query.append("&");
                }
            }
        }

        String vnpSecureHash = VnPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        query.append("&vnp_SecureHash=").append(vnpSecureHash);

        return vnPayConfig.getPayUrl() + "?" + query;
    }

    public int orderReturn(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        Enumeration<String> params = request.getParameterNames();

        while (params.hasMoreElements()) {
            String name = params.nextElement();
            String value = request.getParameter(name);
            if (value != null && !value.isEmpty()) {
                fields.put(name, value);
            }
        }

        String vnpSecureHash = request.getParameter("vnp_SecureHash");
        String vnpTransactionStatus = request.getParameter("vnp_TransactionStatus");

        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();

        for (int i = 0; i < fieldNames.size(); i++) {
            String name = fieldNames.get(i);
            String value = fields.get(name);
            try {
                hashData.append(name).append("=")
                        .append(URLEncoder.encode(value, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("UTF-8 encoding not supported", e);
            }
            if (i < fieldNames.size() - 1) {
                hashData.append("&");
            }
        }

        if (vnpSecureHash == null || vnpSecureHash.isEmpty()) {
            return -2;
        }

        String signValue = VnPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        if (!signValue.equals(vnpSecureHash)) {
            return -1;
        }

        if ("00".equals(vnpTransactionStatus)) {
            return 1;
        } else {
            return 0;
        }
    }
}
