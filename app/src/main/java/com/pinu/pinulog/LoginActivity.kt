package com.pinu.gpslanglongtest

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.toast

class LoginActivity :AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    val RC_SIGN_IN = 9001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        //파이어베이스 통합 관리 객체 생성
        auth = FirebaseAuth.getInstance()

        btn_google_login.setOnClickListener {
            googleLogin()
        }
        btn_email_login.setOnClickListener{
            emailLogin()
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN && resultCode==Activity.RESULT_OK) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            println(result?.status.toString())
            if (result!!.isSuccess) {
                val account = result!!.signInAccount
                firebaseAuthWithGoogle(account!!)
            } else {
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val user = auth.currentUser
                    Firebase.database.reference.child("users").child(user?.uid.toString()).child("email").setValue(user?.email.toString())

                    moveMain(user)
                } else {
                    val loginfailalert = AlertDialog.Builder(this)
                    loginfailalert.setMessage("Try later...")
                    loginfailalert.setPositiveButton("Ok", null)
                    loginfailalert.show()
                    moveMain(null)
                }
            }
    }

    public override fun onStart() {
        super.onStart()
        //유저가 로그인되어있는지 확인.. 자동로그인
        val currentUser = auth.currentUser
        moveMain(currentUser)
    }

    //유저가 로그인하면 메인액티비티로 이동
    private fun moveMain(user: FirebaseUser?) {
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    fun googleLogin() {
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    fun createAndLoginEmail(){
        auth.createUserWithEmailAndPassword(email.text.toString(), password.text.toString()).addOnCompleteListener(this){ task ->
            if(task.isSuccessful){
                val user = auth.currentUser
                Firebase.database.reference.child("users").child(user?.uid.toString()).child("email").setValue(user?.email.toString())
                moveMain(user)
            }
            else if (task.exception?.message.isNullOrEmpty()){
                Toast.makeText(this,task.exception!!.message,Toast.LENGTH_SHORT).show()
            }
            else{
                signInEmail()
            }
        }
    }

    fun emailLogin(){
        if(email.text.toString().isNullOrEmpty()||password.text.toString().isNullOrEmpty()){
            toast("Enter your email or password")
        } else{
            createAndLoginEmail()
        }
    }

    fun signInEmail(){
        auth.signInWithEmailAndPassword(email.text.toString(),password.text.toString()).addOnCompleteListener{ task->
            if (task.isSuccessful){
                moveMain(auth.currentUser)
            }
            else{
                Toast.makeText(this,task.exception!!.message,Toast.LENGTH_SHORT).show()
            }

        }

    }
}