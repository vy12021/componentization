package com.bhb.android.plugin.componentization

import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.security.MessageDigest

internal fun fileMD5(input: File, upperCase: Boolean): String {
  input.inputStream().use {
    val mdInst = MessageDigest.getInstance("MD5")
    val buffer: ByteBuffer = it.channel.map(FileChannel.MapMode.READ_ONLY, 0, input.length())
    mdInst.update(buffer)
    return bytes2Hex(mdInst.digest(), upperCase)
  }
}

internal fun bytes2Hex(input: ByteArray, upperCase: Boolean): String {
  val sb = StringBuilder()
  var hex: String
  for (i in input.indices) {
    hex = Integer.toHexString(input[i].toUInt().toInt())
    if (hex.length == 1) {
      sb.append('0')
    }
    sb.append(if (upperCase) hex.toUpperCase() else hex)
  }
  return sb.toString()
}

internal fun hex2Bytes(input: String): ByteArray {
  val output = ByteArray(input.length / 2)
  for (i in 0 until input.length / 2) {
    val high = input.substring(i * 2, i * 2 + 1).toInt(16)
    val low = input.substring(i * 2 + 1, i * 2 + 2).toInt(16)
    output[i] = (high * 16 + low).toByte()
  }
  return output
}