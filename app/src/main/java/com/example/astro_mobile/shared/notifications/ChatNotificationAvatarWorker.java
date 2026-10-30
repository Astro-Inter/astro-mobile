package com.example.astro_mobile.shared.notifications;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.example.astro_mobile.data.api.FailureKind;
import com.example.astro_mobile.data.api.HumanChatRepository;
import com.example.astro_mobile.data.api.dto.ChatConversationData;
import com.example.astro_mobile.data.api.dto.ChatPushData;
import com.example.astro_mobile.data.firebase.PushTokenRepository;
import com.google.android.gms.tasks.Tasks;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Carrega a foto autenticada sem depender da duração do callback do FCM. */
public final class ChatNotificationAvatarWorker extends Worker {
    public ChatNotificationAvatarWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    static void enqueue(Context context, ChatPushData message) {
        // O trabalho persiste somente identificadores, sem token, nome ou prévia da mensagem.
        Data identifiers = new Data.Builder().putString("uid", message.uid)
                .putString("conversation", message.conversationId).putString("message", message.messageId)
                .putLong("sender", message.senderId).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ChatNotificationAvatarWorker.class)
                .setInputData(identifiers)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build();
        WorkManager.getInstance(context).enqueueUniqueWork("chat-avatar:" + message.uid + ":" + message.conversationId,
                ExistingWorkPolicy.REPLACE, request);
    }

    @NonNull @Override public Result doWork() {
        Context context = getApplicationContext();
        String uid = getInputData().getString("uid");
        String conversationId = getInputData().getString("conversation");
        String messageId = getInputData().getString("message");
        long senderId = getInputData().getLong("sender", 0);
        if (uid == null || conversationId == null || messageId == null || senderId <= 0) return Result.failure();
        HumanChatRepository repository = HumanChatRepository.createDefault();
        FutureTarget<Bitmap> target = null;
        try {
            Tasks.await(PushTokenRepository.initialize(context).sessionInitialized(), 5, TimeUnit.SECONDS);
            if (isStopped() || !ChatPushNotifications.canUpdateAvatar(context, uid, conversationId, messageId)) return Result.success();
            CompletableFuture<List<ChatConversationData>> response = new CompletableFuture<>();
            repository.load(new HumanChatRepository.ResultCallback() {
                @Override public void onSuccess(List<ChatConversationData> conversations) { response.complete(conversations); }
                @Override public void onFailure(FailureKind failure) {
                    if (failure == FailureKind.SESSION) response.complete(java.util.Collections.emptyList());
                    else response.completeExceptionally(new IllegalStateException("Avatar lookup unavailable"));
                }
            });
            for (ChatConversationData conversation : response.get(20, TimeUnit.SECONDS)) {
                if (!conversationId.equals(conversation.id) || !Long.valueOf(senderId).equals(conversation.participant.id)) continue;
                String url = conversation.participant.profilePhotoUrl;
                if (url == null || !"https".equalsIgnoreCase(Uri.parse(url.trim()).getScheme())) return Result.success();
                if (isStopped() || !ChatPushNotifications.canUpdateAvatar(context, uid, conversationId, messageId)) return Result.success();
                target = Glide.with(context).asBitmap().load(url.trim()).timeout(4000).circleCrop().submit(192, 192);
                Bitmap photo = target.get(5, TimeUnit.SECONDS).copy(Bitmap.Config.ARGB_8888, false);
                if (!isStopped()) ChatPushNotifications.updateAvatar(context, uid, conversationId, messageId, photo);
                return Result.success();
            }
            return Result.success();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return Result.retry();
        } catch (Exception unavailable) {
            return getRunAttemptCount() < 2 ? Result.retry() : Result.success();
        } finally {
            new Handler(Looper.getMainLooper()).post(repository::cancel);
            if (target != null) Glide.with(context).clear(target);
        }
    }
}
