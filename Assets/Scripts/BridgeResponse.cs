using System;

// BridgeResponse mirrors the Kotlin BridgeResponse data class in
// app/src/main/java/com/example/nshutiplanner/unity/BridgeResponse.kt.
//
// JSON schema:
// {
//   "success":     bool,
//   "latitude":    double,
//   "longitude":   double,
//   "displayName": string,
//   "error":       string
// }
//
// Field rules:
//   success = true  → error is empty; latitude/longitude are valid; displayName is non-empty.
//   success = false → error is non-empty; latitude/longitude/displayName are zero/empty.
//
// Deserialized via JsonUtility.FromJson<BridgeResponse>(json).
[Serializable]
public class BridgeResponse
{
    public bool success;
    public double latitude;
    public double longitude;
    public string displayName;
    public string error;
}
