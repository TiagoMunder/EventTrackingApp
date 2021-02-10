package pt.ubi.eventtrackingapp;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ImageTabsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ImageTabsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImageTabsFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "geoPoint";
    private static final String ARG_PARAM2 = "isOwnerOfImage";
    private static final String ARG_PARAM3= "filteredImages";


    private CustomGeoPoint geoPoint;
    private ImageMarkerClusterItem imageMarker;
    private boolean isOwnerOfImage;
    private ArrayList<MarkerObject> filteredImages = new ArrayList<>();

    private OnFragmentInteractionListener mListener;

    public ImageTabsFragment() {
        // Required empty public constructor
    }


    // TODO: Rename and change types and number of parameters
    public static ImageTabsFragment newInstance(CustomGeoPoint geoPoint, String imageMarker, boolean isOwnerOfImage, ArrayList<MarkerObject> filteredImages) {
        ImageTabsFragment fragment = new ImageTabsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PARAM1, geoPoint);
        args.putBoolean(ARG_PARAM2, isOwnerOfImage);
        args.putParcelableArrayList(ARG_PARAM3, filteredImages);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            geoPoint = getArguments().getParcelable(ARG_PARAM1);

            isOwnerOfImage = getArguments().getBoolean(ARG_PARAM2);
            filteredImages = getArguments().getParcelableArrayList(ARG_PARAM3);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_image_tabs, container, false);
        ViewPager viewPager = (ViewPager)rootView.findViewById(R.id.viewPager);
        TabLayout tabLayout = (TabLayout)rootView.findViewById(R.id.tabLayout);


        viewPager.setAdapter(new FragmentPagerAdapter(getChildFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                Bundle args = new Bundle();
                args.putParcelable("geoPoint", geoPoint);
                args.putBoolean("isNewImage", false);
                args.putBoolean("isOwnerOfImage", isOwnerOfImage);
                args.putParcelableArrayList("filteredImages", filteredImages);
                args.putInt("currentPosition", position);
                MarkerFragment fragment = new MarkerFragment();

                fragment.setArguments(args);
                return fragment;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return position+"";
            }

            @Override
            public int getCount() {
                return filteredImages.size();
            }
        });

        tabLayout.setupWithViewPager(viewPager);
        return rootView;
        // Inflate the layout for this fragment

    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
