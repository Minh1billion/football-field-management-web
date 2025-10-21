package utescore.dto;

import lombok.Data;

@Data
public class AccountDTO {
    private Long id;
    private String username;
    private String email;
    private String role;
}