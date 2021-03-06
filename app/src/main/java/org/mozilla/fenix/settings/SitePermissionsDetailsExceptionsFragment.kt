/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mozilla.components.feature.sitepermissions.SitePermissions
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.settings.PhoneFeature.CAMERA
import org.mozilla.fenix.settings.PhoneFeature.MICROPHONE
import org.mozilla.fenix.settings.PhoneFeature.LOCATION
import org.mozilla.fenix.settings.PhoneFeature.NOTIFICATION
import kotlin.coroutines.CoroutineContext

@SuppressWarnings("TooManyFunctions")
class SitePermissionsDetailsExceptionsFragment : PreferenceFragmentCompat(), CoroutineScope {
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext get() = Dispatchers.IO + job
    private lateinit var sitePermissions: SitePermissions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.show()

        sitePermissions = SitePermissionsDetailsExceptionsFragmentArgs
            .fromBundle(requireArguments())
            .sitePermissions

        (activity as AppCompatActivity).title = sitePermissions.origin
        job = Job()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.site_permissions_details_exceptions_preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        launch(IO) {
            val context = requireContext()
            sitePermissions = requireNotNull(context.components.storage.findSitePermissionsBy(sitePermissions.origin))
            launch(Main) {
                bindCategoryPhoneFeatures()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun bindCategoryPhoneFeatures() {
        val context = requireContext()

        val cameraAction = CAMERA.getActionLabel(context, sitePermissions)
        val locationAction = LOCATION.getActionLabel(context, sitePermissions)
        val microphoneAction = MICROPHONE.getActionLabel(context, sitePermissions)
        val notificationAction = NOTIFICATION.getActionLabel(context, sitePermissions)

        initPhoneFeature(CAMERA, cameraAction)
        initPhoneFeature(LOCATION, locationAction)
        initPhoneFeature(MICROPHONE, microphoneAction)
        initPhoneFeature(NOTIFICATION, notificationAction)
        bindClearPermissionsButton()
    }

    private fun initPhoneFeature(phoneFeature: PhoneFeature, summary: String) {
        val keyPreference = phoneFeature.getPreferenceKey(requireContext())
        val cameraPhoneFeatures: Preference = requireNotNull(findPreference(keyPreference))
        cameraPhoneFeatures.summary = summary

        cameraPhoneFeatures.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            navigateToPhoneFeature(phoneFeature)
            true
        }
    }

    private fun bindClearPermissionsButton() {
        val keyPreference = getString(R.string.pref_key_exceptions_clear_site_permissions)
        val button: Preference = requireNotNull(findPreference(keyPreference))

        button.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            requireContext().alert(
                R.string.confirm_clear_permissions_site,
                R.string.clear_permissions
            ) {
                yesButton {
                    clearSitePermissions()
                }
                noButton { }
            }.show()

            true
        }
    }

    private fun clearSitePermissions() {
        launch(IO) {
            requireContext().components.storage.deleteSitePermissions(sitePermissions)
            launch(Main) {
                Navigation.findNavController(requireNotNull(view)).popBackStack()
            }
        }
    }

    private fun navigateToPhoneFeature(phoneFeature: PhoneFeature) {
        val directions =
            SitePermissionsDetailsExceptionsFragmentDirections.actionSitePermissionsToExceptionsToManagePhoneFeature(
                phoneFeatureId = phoneFeature.id,
                sitePermissions = sitePermissions
            )
        Navigation.findNavController(view!!).navigate(directions)
    }
}
