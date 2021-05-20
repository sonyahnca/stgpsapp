package com.pinu.pinulog

import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsFragment : Fragment(), OnMapReadyCallback{

    private var longitude: Double = 0.0
    private var latitude: Double = 0.0
    private var markedTime: String? = null

    private lateinit var mMap: GoogleMap
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?{

        arguments?.let{
            longitude = it.getDouble("flongitude")
            latitude = it.getDouble("flatitude")
            markedTime = it.getString("fmarkedTime")
        }

        val rootView: View = inflater.inflate(R.layout.fragment_maps, container, false)
        val mapsFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapsFragment.getMapAsync(this)

        return rootView
    }

    override fun onMapReady(googleMap: GoogleMap){
        mMap=googleMap
        val marker = LatLng(latitude,longitude)
        mMap.addMarker(MarkerOptions().position(marker).title("$markedTime"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker,10f))
    }

}


