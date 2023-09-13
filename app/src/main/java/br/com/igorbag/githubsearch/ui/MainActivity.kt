package br.com.igorbag.githubsearch.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.igorbag.githubsearch.R
import br.com.igorbag.githubsearch.data.GitHubService
import br.com.igorbag.githubsearch.domain.Repository
import br.com.igorbag.githubsearch.ui.adapter.RepositoryAdapter
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    lateinit var etNomeUsuario: EditText
    lateinit var btnConfirmar: Button
    lateinit var listaRepositories: RecyclerView
    lateinit var githubApi: GitHubService
    lateinit var repositoryAdapter: RepositoryAdapter
    lateinit var btnNovaConsulta: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupView()
        setupListeners()
        showUserName()
        setupRetrofit()

        // Limpa o campo nome do usuario ao iniciar o app
        etNomeUsuario.text.clear()
    }

    // Metodo responsavel por realizar o setup da view e recuperar os Ids do layout
    fun setupView() {
        etNomeUsuario = findViewById(R.id.et_nome_usuario)
        btnConfirmar = findViewById(R.id.btn_confirmar)
        listaRepositories = findViewById(R.id.rv_lista_repositories)
        btnNovaConsulta = findViewById(R.id.btn_nova_consulta)
    }

    // Metodo responsavel por configurar os listeners click da tela
    private fun setupListeners() {
        btnConfirmar.setOnClickListener{
            val dataNomeUsuario = etNomeUsuario.text.toString()

            // Condicao caso o botao confirmar seja clicado sem ter sido digitado algum usuario
            if (dataNomeUsuario.isBlank()) {
                Toast.makeText(this@MainActivity, "Por favor, insira um nome de usuário!", Toast.LENGTH_SHORT).show()
            } else {
                // O campo nao esta vazio, continuar o codigo
                saveUserLocal(dataNomeUsuario)
                getAllReposByUserName()
            }
        }

        btnNovaConsulta.setOnClickListener {
            // Limpa o campo nome do usuario
            etNomeUsuario.text.clear()

            // Limpa a lista de repositorios
            clearRepositoryList()
        }
    }

    private fun clearRepositoryList() {
        val emptyList = emptyList<Repository>()
        setupAdapter(emptyList)
    }

    // Salvar o usuario preenchido no EditText utilizando uma SharedPreferences
    private fun saveUserLocal(nome: String) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()){
            putString(getString(R.string.sp_nome_usuario), nome)
            apply()
        }
    }

    private fun showUserName() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val nome = sharedPref.getString(getString(R.string.sp_nome_usuario), "")

        if (nome != ""){
            etNomeUsuario.setText(nome)
        }
    }

    // Metodo responsavel por fazer a configuracao base do Retrofit
    fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        githubApi = retrofit.create(GitHubService::class.java)
    }

    // Metodo responsavel por buscar todos os repositorios do usuario fornecido
    fun getAllReposByUserName() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val nome = sharedPref.getString(getString(R.string.sp_nome_usuario), "")

        if (nome != null && nome != ""){
            val call = githubApi.getAllRepositoriesByUser(nome!!)
            call.enqueue(object :Callback<List<Repository>>{
                override fun onResponse(call: Call<List<Repository>>, response: Response<List<Repository>>) {
                    if (response.isSuccessful){
                        response.body()?.let {
                            setupAdapter(it)
                        }
                    }else{
                        Toast.makeText(applicationContext, getString(R.string.erro_api), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<Repository>>, t: Throwable) {
                    Toast.makeText(applicationContext, getString(R.string.erro_api), Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    // Metodo responsavel por realizar a configuracao do adapter
    fun setupAdapter(list: List<Repository>) {
        repositoryAdapter = RepositoryAdapter(list)
        listaRepositories.adapter = repositoryAdapter
        listaRepositories.adapter = repositoryAdapter

        repositoryAdapter.repositoryItemLister = {
            openBrowser(it.htmlUrl)
        }

        repositoryAdapter.btnShareLister = {
            shareRepositoryLink(it.htmlUrl)
        }
    }

    // Metodo responsavel por compartilhar o link do repositorio selecionado
    fun shareRepositoryLink(urlRepository: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, urlRepository)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    // Metodo responsavel por abrir o browser com o link informado do repositorio
    fun openBrowser(urlRepository: String) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(urlRepository)
            )
        )
    }
}