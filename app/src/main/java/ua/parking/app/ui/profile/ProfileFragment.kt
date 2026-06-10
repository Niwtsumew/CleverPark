package ua.parking.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import ua.parking.app.R
import ua.parking.app.data.repository.AuthRepositoryImpl
import ua.parking.app.databinding.FragmentProfileBinding
import ua.parking.app.ui.auth.AuthViewModel

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private val authRepo = AuthRepositoryImpl()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        binding.tvEmail.text = user?.email ?: ""

        profileViewModel.loadBalance()

        profileViewModel.balance.observe(viewLifecycleOwner) { balance ->
            binding.tvBalance.text = "%.2f грн".format(balance)
        }

        binding.btnTopup50.setOnClickListener  { profileViewModel.topUp(50.0) }
        binding.btnTopup100.setOnClickListener { profileViewModel.topUp(100.0) }
        binding.btnTopup200.setOnClickListener { profileViewModel.topUp(200.0) }
        binding.btnTopup500.setOnClickListener { profileViewModel.topUp(500.0) }

        binding.btnSignOut.setOnClickListener {
            authViewModel.signOut()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }

        binding.btnAdmin.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_adminPanelFragment)
        }

        checkAdminAccess()
    }

    private fun checkAdminAccess() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        authViewModel.viewModelScope.launch {
            val isAdmin = authRepo.isAdmin(uid)
            binding.btnAdmin.visibility = if (isAdmin) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
