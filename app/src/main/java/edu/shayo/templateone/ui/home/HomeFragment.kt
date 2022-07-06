package edu.shayo.templateone.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import edu.shayo.templateone.databinding.FragmentHomeBinding

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding
    get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.homeFragmentStartButton.setOnClickListener {
            //homeViewModel.navigate()

            (requireActivity() as ServiceController).startService()
        }

        binding.homeFragmentStopButton.setOnClickListener {
            //homeViewModel.navigate()

            (requireActivity() as ServiceController).stopService()
        }

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()

        _binding = null
    }
}

interface ServiceController {
    fun startService()

    fun stopService()
}