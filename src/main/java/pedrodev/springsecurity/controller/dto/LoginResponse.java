package pedrodev.springsecurity.controller.dto;

public record LoginResponse(String accessToken, Long expiresIn) {
}
