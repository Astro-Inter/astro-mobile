package com.example.astro_mobile.auth;

import static org.junit.Assert.assertEquals;

import com.example.astro_mobile.data.api.EmailVerificationRepository;
import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.VerifyEmailData;
import com.google.gson.Gson;

import org.junit.Test;

import retrofit2.Call;

public class EmailVerificationViewModelTest {
    private final Gson gson = new Gson();

    @Test
    public void disabledAccountIsBlockedForEveryUserType() {
        for (String userType : new String[] {"COLABORADOR", "GESTOR", "GESTOR_WORKSPACE"}) {
            assertEquals(EmailVerificationViewModel.Destination.DISABLED,
                    destinationFor(userType, "DESATIVADO"));
        }
    }

    @Test
    public void activeAndPreRegisteredAccountsGoToExpectedScreens() {
        assertEquals(EmailVerificationViewModel.Destination.PASSWORD,
                destinationFor("GESTOR", "ATIVO"));
        assertEquals(EmailVerificationViewModel.Destination.PASSWORD,
                destinationFor("COLABORADOR", "ATIVO"));
        assertEquals(EmailVerificationViewModel.Destination.FIRST_ACCESS_KEY,
                destinationFor("COLABORADOR", "PRE_CADASTRADO"));
        assertEquals(EmailVerificationViewModel.Destination.INTERNAL_ERROR,
                destinationFor("UNKNOWN", "ATIVO"));
        assertEquals(EmailVerificationViewModel.Destination.INTERNAL_ERROR,
                destinationFor("GESTOR", "UNKNOWN"));
    }

    private EmailVerificationViewModel.Destination destinationFor(String userType, String status) {
        String json = "{\"userType\":\"" + userType + "\",\"userStatus\":\"" + status + "\"}";
        VerifyEmailData data = gson.fromJson(json, VerifyEmailData.class);
        EmailVerificationRepository repository = new EmailVerificationRepository(null) {
            @Override
            public Call<ApiResponse<VerifyEmailData>> verifyEmail(String email,
                    ResultCallback callback) {
                callback.onSuccess(data);
                return null;
            }
        };
        EmailVerificationViewModel model = new EmailVerificationViewModel(repository);
        EmailVerificationViewModel.Destination[] result = new EmailVerificationViewModel.Destination[1];
        model.verifyEmail("teste@example.com", (destination, message, type) -> result[0] = destination);
        return result[0];
    }
}
