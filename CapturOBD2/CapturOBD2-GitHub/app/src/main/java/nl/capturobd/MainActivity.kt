package nl.capturobd

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : Activity() {
 private val spp = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
 private var socket: BluetoothSocket? = null
 private var input: InputStream? = null
 private var output: OutputStream? = null
 private lateinit var status: TextView
 private lateinit var log: TextView
 private lateinit var deviceList: Spinner
 private lateinit var connectBtn: Button
 private val devices = mutableListOf<BluetoothDevice>()
 private val adapter by lazy { BluetoothAdapter.getDefaultAdapter() }

 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(24,24,24,24); setBackgroundColor(0xFF102A43.toInt()) }
  fun text(s:String, size:Float=16f)=TextView(this).apply { text=s; textSize=size; setTextColor(0xFFFFFFFF.toInt()); setPadding(0,8,0,8) }
  root.addView(text("CAPTUR OBD DIAGNOSE",24f))
  root.addView(text("Renault Captur • ELM327 Bluetooth"))
  status=text("Status: niet verbonden"); root.addView(status)
  deviceList=Spinner(this); root.addView(deviceList)
  val scan=Button(this).apply { text="Gekoppelde OBD-adapters" }; root.addView(scan)
  connectBtn=Button(this).apply { text="Verbinden" }; root.addView(connectBtn)
  val read=Button(this).apply { text="Foutcodes uitlezen" }; root.addView(read)
  val live=Button(this).apply { text="Live motordata" }; root.addView(live)
  val clear=Button(this).apply { text="Foutcodes wissen (04)" }; root.addView(clear)
  log=text("Logboek\n",14f); log.setTextColor(0xFFB8D8E8.toInt()); root.addView(ScrollView(this).apply { addView(log) }, LinearLayout.LayoutParams(-1,0,1f))
  setContentView(root)
  scan.setOnClickListener { listPaired() }
  connectBtn.setOnClickListener { connectSelected() }
  read.setOnClickListener { runCommands(listOf("03"), "Foutcodes") }
  live.setOnClickListener { runCommands(listOf("010C","010D","0105","0104"), "Live data") }
  clear.setOnClickListener { AlertDialogBuilder().confirmClear() }
  listPaired()
 }

 private fun hasPermission(): Boolean = Build.VERSION.SDK_INT < 31 || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED
 private fun requestBt() { if(Build.VERSION.SDK_INT>=31 && !hasPermission()) requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN),7) }
 private fun listPaired() {
  requestBt(); if(!hasPermission()) { status.text="Bluetooth-permissie vereist"; return }
  try {
   devices.clear(); devices.addAll(adapter?.bondedDevices?.filter { it.name?.contains("OBD",true)==true || it.name?.contains("ELM",true)==true } ?: emptySet())
   if(devices.isEmpty()) devices.addAll(adapter?.bondedDevices ?: emptySet())
   deviceList.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,devices.map { "${it.name ?: "Bluetooth"} • ${it.address}" })
   status.text="${devices.size} gekoppelde apparaten gevonden"
  } catch(e:SecurityException){ status.text="Bluetooth-permissie ontbreekt" }
 }
 private fun connectSelected() {
  if(!hasPermission()){requestBt();return}; if(devices.isEmpty()){status.text="Koppel eerst ELM327 in Android Bluetooth-instellingen";return}
  val d=devices.getOrNull(deviceList.selectedItemPosition) ?: return
  status.text="Verbinden…"
  thread { try {
   adapter?.cancelDiscovery(); val s=d.createRfcommSocketToServiceRecord(spp); s.connect(); socket=s; input=s.inputStream; output=s.outputStream
   val init=listOf("ATZ","ATE0","ATL0","ATS0","ATH0","ATSP0")
   init.forEach { send(it) }
   runOnUiThread { status.text="Verbonden: ${d.name}"; append("Bluetoothverbinding actief; ELM ingesteld.") }
  } catch(e:Exception){runOnUiThread { status.text="Verbinding mislukt: ${e.message}"; append("Controleer adapter, pairing en contact.") }} }
 }
 private fun send(cmd:String):String {
  val out=output ?: return "Niet verbonden"; val ins=input ?: return "Niet verbonden"
  synchronized(this){ out.write((cmd+"\r").toByteArray());out.flush(); val buf=ByteArray(1024); val sb=StringBuilder(); val start=System.currentTimeMillis();
   while(System.currentTimeMillis()-start<2500){ if(ins.available()>0){val n=ins.read(buf);if(n>0){sb.append(String(buf,0,n));if(sb.contains(">"))break}}else Thread.sleep(40) }; return sb.toString().replace(">", "").trim() }
 }
 private fun runCommands(cmds:List<String>, title:String){ if(socket==null){status.text="Verbind eerst met de adapter";return};thread { val results=cmds.map { it to send(it) };runOnUiThread { append("\n$title:");results.forEach { append("${it.first}: ${it.second}") } } } }
 private fun append(s:String){ log.text=log.text.toString()+"\n"+s }
 private inner class AlertDialogBuilder { fun confirmClear(){ android.app.AlertDialog.Builder(this@MainActivity).setTitle("Foutcodes wissen?").setMessage("Dit stuurt OBD-commando 04. Codes, freeze-frame data en emissie-readiness kunnen worden gewist. Dit repareert de oorzaak niet. Wis alleen na opslaan van de codes en bij stilstaande auto.").setNegativeButton("Annuleren",null).setPositiveButton("Wissen") { _,_ -> runCommands(listOf("04"),"Wiscommando") }.show() } }
}
