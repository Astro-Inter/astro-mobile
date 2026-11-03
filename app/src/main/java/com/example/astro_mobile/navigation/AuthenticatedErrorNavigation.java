package com.example.astro_mobile.navigation;

import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavBackStackEntry;

import com.example.astro_mobile.R;
import com.example.astro_mobile.shared.navigation.HomeNavigation;

/** Retorno e nova tentativa preservando a tela autenticada já carregada. */
final class AuthenticatedErrorNavigation {
    private AuthenticatedErrorNavigation() { }

    static boolean bind(Fragment fragment, View view, NavController navController,
                        NavBackStackEntry previous, int source, int errorDestination) {
        if (source == R.id.employeeEventsFragment || source == R.id.employeeHomeFragment
                || source == R.id.managerHomeFragment || source == R.id.notificationsFragment
                || source == R.id.aiSessionsFragment || source == R.id.chatFragment
                || source == R.id.employeeEventDetailsFragment || source == R.id.conversationsFragment
                || source == R.id.humanChatFragment || source == R.id.newConversationFragment
                || source == R.id.managerFormsFragment || source == R.id.formDetailsFragment
                || source == R.id.formAnswerFragment || source == R.id.managerEventsFragment
                || source == R.id.staffFragment || source == R.id.staffDetailsFragment
                || source == R.id.staffEventsFragment
                || source == R.id.eventParticipantsFragment || source == R.id.managerEventDetailsFragment
                || source == R.id.eventAssignmentFragment || source == R.id.eventValidationFragment) {
            // Retoma a consulta da tela autenticada, sem repetir a identificação de e-mail.
            ErrorFragment.showCopy(view.findViewById(R.id.text_email_error_title),
                    view.findViewById(R.id.text_email_error_body),
                    errorDestination == R.id.connectionErrorFragment);
            view.findViewById(R.id.button_email_error_retry).setOnClickListener(clicked -> {
                String conversation = fragment.getArguments() == null ? null : fragment.getArguments().getString("retry_ai_conversation");
                if (source == R.id.aiSessionsFragment && conversation != null) {
                    navController.getBackStackEntry(source).getSavedStateHandle()
                            .set("retry_ai_conversation", conversation);
                } else if (source == R.id.conversationsFragment || source == R.id.newConversationFragment) {
                    previous.getSavedStateHandle().set("retry_human_chat_list", true);
                } else if (source == R.id.humanChatFragment) {
                    previous.getSavedStateHandle().set("retry_human_chat_history", true);
                }
                if (source == R.id.staffFragment) {
                    navController.getBackStackEntry(source).getSavedStateHandle()
                            .set("retry_staff_list", true);
                }
                if (source == R.id.staffDetailsFragment) {
                    previous.getSavedStateHandle().set("retry_staff_details", true);
                }
                if (source == R.id.staffEventsFragment) {
                    previous.getSavedStateHandle().set("retry_staff_events", true);
                }
                if (source == R.id.eventParticipantsFragment) {
                    previous.getSavedStateHandle().set("retry_event_participants", true);
                }
                if (source == R.id.managerEventDetailsFragment) {
                    previous.getSavedStateHandle().set("retry_manager_event_details", true);
                }
                if (source == R.id.eventAssignmentFragment && fragment.getArguments() != null) {
                    previous.getSavedStateHandle().set("retry_event_assignment", fragment.getArguments().getString("retry_event_assignment"));
                }
                if (source == R.id.eventValidationFragment && fragment.getArguments() != null) {
                    previous.getSavedStateHandle().set("retry_event_validation", fragment.getArguments().getString("retry_event_validation"));
                }
                navController.popBackStack(source, false);
            });
            TextView back = view.findViewById(R.id.button_email_error_start);
            back.setText(R.string.profile_retry_back);
            ErrorFragment.configureBack(fragment, view, () -> {
                if (source == R.id.staffDetailsFragment) {
                    boolean openedFromChat = fragment.getArguments() != null
                            && fragment.getArguments().getBoolean(com.example.astro_mobile.manager.staff.StaffDetailsFragment.ARG_OPENED_FROM_CHAT);
                    if (openedFromChat) navController.popBackStack(R.id.humanChatFragment, false);
                    else navController.popBackStack(R.id.staffFragment, false);
                }
                else if (source == R.id.staffEventsFragment) navController.popBackStack(R.id.staffDetailsFragment, false);
                else if (source == R.id.eventParticipantsFragment) navController.popBackStack(R.id.managerEventDetailsFragment, false);
                else if (source == R.id.managerEventDetailsFragment) navController.popBackStack(R.id.managerEventDetailsFragment, true);
                else if (source == R.id.eventAssignmentFragment) navController.popBackStack(R.id.eventAssignmentFragment, true);
                else if (source == R.id.eventValidationFragment) navController.popBackStack(R.id.eventValidationFragment, false);
                else if (source == R.id.humanChatFragment) navController.popBackStack(R.id.conversationsFragment, false);
                else HomeNavigation.returnToLoadedHome(fragment);
            });
            return true;
        }
        return false;
    }
}
