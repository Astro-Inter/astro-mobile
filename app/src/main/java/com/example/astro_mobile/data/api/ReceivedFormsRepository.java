package com.example.astro_mobile.data.api;

import com.example.astro_mobile.data.api.dto.ApiResponse;
import com.example.astro_mobile.data.api.dto.ReceivedFormsPage;
import com.example.astro_mobile.data.firebase.FirebaseIdTokenProvider;
import com.example.astro_mobile.data.firebase.IdTokenProvider;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class ReceivedFormsRepository {
    public interface ResultCallback {
        void onSuccess(List<ReceivedFormsPage.Item> forms);
        void onFailure(FailureKind failure);
    }
    private final AstroApi api;
    private final IdTokenProvider tokens;
    private Call<ApiResponse<ReceivedFormsPage>> currentCall;
    private int requestId;

    public ReceivedFormsRepository(AstroApi api, IdTokenProvider tokens) {
        this.api = api;
        this.tokens = tokens;
    }
    public static ReceivedFormsRepository createDefault() {
        return new ReceivedFormsRepository(AstroApiClient.getApi(), FirebaseIdTokenProvider.createDefault());
    }
    public void load(ResultCallback callback) {
        cancel();
        loadPage(0, new ArrayList<>(), false, requestId, callback);
    }
    private void loadPage(int page, List<ReceivedFormsPage.Item> forms, boolean refresh,
                          int id, ResultCallback callback) {
        tokens.getToken(refresh, new IdTokenProvider.Callback() {
            @Override public void onToken(String token) {
                if (id != requestId) return;
                currentCall = api.getReceivedForms("Bearer " + token, page, 100);
                currentCall.enqueue(new Callback<ApiResponse<ReceivedFormsPage>>() {
                    @Override public void onResponse(Call<ApiResponse<ReceivedFormsPage>> call,
                                                      Response<ApiResponse<ReceivedFormsPage>> response) {
                        if (id != requestId) return;
                        currentCall = null;
                        // Renova uma vez; 403 representa acesso negado, não expiração.
                        if (response.code() == 401) {
                            if (refresh) callback.onFailure(FailureKind.SESSION);
                            else loadPage(page, forms, true, id, callback);
                            return;
                        }
                        ApiResponse<ReceivedFormsPage> body = response.body();
                        ReceivedFormsPage data = body == null ? null : body.getData();
                        if (response.code() != 200 || body == null || !body.isSuccess()
                                || data == null || data.items == null || data.page != page) {
                            callback.onFailure(FailureKind.INTERNAL);
                            return;
                        }
                        // A pesquisa local precisa considerar todas as páginas, não só os primeiros resultados.
                        forms.addAll(data.items);
                        if (page + 1L < data.totalPages) loadPage(page + 1, forms, false, id, callback);
                        else callback.onSuccess(forms);
                    }
                    @Override public void onFailure(Call<ApiResponse<ReceivedFormsPage>> call, Throwable error) {
                        if (id != requestId) return;
                        currentCall = null;
                        callback.onFailure(ApiFailures.transport(error));
                    }
                });
            }
            @Override public void onFailure(IdTokenProvider.Failure failure) {
                if (id != requestId) return;
                callback.onFailure(failure == IdTokenProvider.Failure.SESSION ? FailureKind.SESSION
                        : failure == IdTokenProvider.Failure.CONNECTION ? FailureKind.CONNECTION : FailureKind.INTERNAL);
            }
        });
    }
    public void cancel() {
        ++requestId;
        if (currentCall != null) currentCall.cancel();
        currentCall = null;
    }
}
