package at.team30.setroute.ui.route_detail

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import at.team30.setroute.Helper.NavigationHelper
import at.team30.setroute.R
import at.team30.setroute.ui.settings.SettingsFragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.stfalcon.imageviewer.StfalconImageViewer
import com.zeugmasolutions.localehelper.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RouteDetailFragment : Fragment() {
    private val viewModel: RouteDetailViewModel by viewModels()
    private val args: RouteDetailFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setId(args.routeId);
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_route_detail, container, false)
    }

    private fun loadImage(view : ImageView, image : Bitmap) {
        view.setImageBitmap(image)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val locale = LocaleHelper.getLocale(requireContext())
        val route = viewModel.getRoute(args.routeId)

        val imageView = view.findViewById<ImageView>(R.id.static_map_view);
        val headerView = view.findViewById<ImageView>(R.id.header)
        val errorView = view.findViewById<ImageView>(R.id.connectionError)
        val name = view.findViewById<TextView>(R.id.title)
        val information = view.findViewById<TextView>(R.id.information)
        val description = view.findViewById<TextView>(R.id.description)

        // shared Preference
        val sharedPreference = activity?.getSharedPreferences(SettingsFragment.SHARED_PREF_KEY, Context.MODE_PRIVATE)
        val milesEnabled = sharedPreference?.getBoolean(SettingsFragment.MILES_PREF_KEY, false) ?: false
        val distanceUnit = if(milesEnabled) getString(R.string.unit_miles) else getString(R.string.unit_km)
        val length = route?.getLength(milesEnabled)

        name.text = route?.getLocalizedName(locale.language)
        information.text = "${String.format("%.2f", length)} ${distanceUnit} / ${route?.duration?.toString() ?: "-"} ${getString(R.string.unit_min)}"
        description.text = route?.getLocalizedDescription(locale.language)

        val navButton = view.findViewById<Button>(R.id.navigation_start_button)
        navButton.setOnClickListener {
            val link = route?.let { it1 -> NavigationHelper.getNavLink(it1) }
            val gmmIntentUri = Uri.parse(link)
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }

        val imageObserver = Observer<Bitmap?> { image ->
            if(image != null){
                imageView.setImageBitmap(image)
                imageView.visibility = View.VISIBLE;
                headerView.visibility = View.INVISIBLE
            }else{
                headerView.visibility = View.INVISIBLE;
                errorView.visibility = View.VISIBLE;
            }
        }
        viewModel.getImageLiveData().observe(viewLifecycleOwner,imageObserver);

        imageView.setOnClickListener {
            StfalconImageViewer.Builder(context, listOf(imageView.drawable.toBitmap()), ::loadImage)
                .withHiddenStatusBar(false)
                .withTransitionFrom(imageView)
                .show()
        }

        val walkedButton = view.findViewById<MaterialButtonToggleGroup>(R.id.walked_button_group);

        walkedButton.addOnButtonCheckedListener { _, _, isChecked ->
            val editor = sharedPreference?.edit()
            editor?.putBoolean("route_walked_${args.routeId}", isChecked)
            editor?.apply()
        }
        if(sharedPreference?.getBoolean("route_walked_${args.routeId}", false) == true)
            walkedButton.check(R.id.walked_button)
        else
            walkedButton.uncheck(R.id.walked_button)
    }
}