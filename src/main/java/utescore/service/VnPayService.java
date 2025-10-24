package utescore.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import utescore.config.VnPayConfig;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnPayService {

    private final VnPayConfig vnPayConfig;

    /**
     * Tạo URL thanh toán VNPay cho đơn hàng mua (Sales)
     */
    public String createOrderForSales(int total, String orderInfo, String baseUrl) {
        log.info("Creating VNPay order for SALES: amount={}, info={}", total, orderInfo);
        return createOrder(total, orderInfo, baseUrl, "/user/sales/payment/callback");
    }

    /**
     * Tạo URL thanh toán VNPay cho đơn hàng thuê (Rentals)
     */
    public String createOrderForRentals(int total, String orderInfo, String baseUrl) {
        log.info("Creating VNPay order for RENTALS: amount={}, info={}", total, orderInfo);
        return createOrder(total, orderInfo, baseUrl, "/user/rentals/payment/callback");
    }

    /**
     * Phương thức cũ để tương thích ngược - mặc định dùng cho rentals
     * @deprecated Sử dụng createOrderForSales() hoặc createOrderForRentals() thay thế
     */
    @Deprecated
    public String createOrder(int total, String orderInfo, String baseUrl) {
        log.warn("Using deprecated createOrder() method - defaulting to RENTALS callback");
        return createOrderForRentals(total, orderInfo, baseUrl);
    }

    /**
     * Tạo URL thanh toán VNPay với callback path tùy chỉnh
     * PUBLIC để có thể gọi trực tiếp nếu cần custom callback path
     */
    public String createOrder(int total, String orderInfo, String baseUrl, String callbackPath) {
        try {
            String vnpVersion = "2.1.0";
            String vnpCommand = "pay";
            String vnpTxnRef = VnPayConfig.getRandomNumber(8);
            String vnpIpAddr = "127.0.0.1";
            String orderType = "other";

            Map<String, String> vnpParams = new TreeMap<>(); // TreeMap tự động sort
            vnpParams.put("vnp_Version", vnpVersion);
            vnpParams.put("vnp_Command", vnpCommand);
            vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
            vnpParams.put("vnp_Amount", String.valueOf(total * 100));
            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_TxnRef", vnpTxnRef);
            vnpParams.put("vnp_OrderInfo", orderInfo);
            vnpParams.put("vnp_OrderType", orderType);
            vnpParams.put("vnp_Locale", "vn");

            String returnUrl = baseUrl + callbackPath;
            log.info("🔗 VNPAY Return URL: {}", returnUrl);
            vnpParams.put("vnp_ReturnUrl", returnUrl);
            vnpParams.put("vnp_IpAddr", vnpIpAddr);

            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            vnpParams.put("vnp_CreateDate", formatter.format(cld.getTime()));
            cld.add(Calendar.MINUTE, 15);
            vnpParams.put("vnp_ExpireDate", formatter.format(cld.getTime()));

            // Build hash data và query string
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            boolean isFirst = true;
            for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue();

                if (value != null && !value.isEmpty()) {
                    if (!isFirst) {
                        hashData.append("&");
                        query.append("&");
                    }

                    String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
                    hashData.append(name).append("=").append(encodedValue);
                    query.append(URLEncoder.encode(name, StandardCharsets.UTF_8))
                            .append("=")
                            .append(encodedValue);

                    isFirst = false;
                }
            }

            String vnpSecureHash = VnPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
            query.append("&vnp_SecureHash=").append(vnpSecureHash);

            String paymentUrl = vnPayConfig.getPayUrl() + "?" + query;
            log.info("✅ VNPay payment URL generated successfully");

            return paymentUrl;

        } catch (Exception e) {
            log.error("❌ Error creating VNPay order: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo URL thanh toán VNPay", e);
        }
    }

    /**
     * Xác thực và xử lý callback từ VNPay
     * @return 1: thành công, 0: thất bại, -1: chữ ký không hợp lệ, -2: thiếu chữ ký
     */
    public int orderReturn(HttpServletRequest request) {
        try {
            Map<String, String> fields = new TreeMap<>();
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
            String vnpTxnRef = request.getParameter("vnp_TxnRef");

            log.info("📥 VNPay callback received: TxnRef={}, Status={}", vnpTxnRef, vnpTransactionStatus);

            if (vnpSecureHash == null || vnpSecureHash.isEmpty()) {
                log.error("❌ VNPay callback missing secure hash");
                return -2;
            }

            fields.remove("vnp_SecureHashType");
            fields.remove("vnp_SecureHash");

            // Build hash data
            StringBuilder hashData = new StringBuilder();
            boolean isFirst = true;

            for (Map.Entry<String, String> entry : fields.entrySet()) {
                if (!isFirst) {
                    hashData.append("&");
                }
                hashData.append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                isFirst = false;
            }

            String signValue = VnPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());

            if (!signValue.equals(vnpSecureHash)) {
                log.error("❌ VNPay signature validation failed");
                return -1;
            }

            if ("00".equals(vnpTransactionStatus)) {
                log.info("✅ VNPay payment successful: TxnRef={}", vnpTxnRef);
                return 1;
            } else {
                log.warn("⚠️ VNPay payment failed: TxnRef={}, Status={}", vnpTxnRef, vnpTransactionStatus);
                return 0;
            }

        } catch (Exception e) {
            log.error("❌ Error processing VNPay callback: {}", e.getMessage(), e);
            return -1;
        }
    }
}